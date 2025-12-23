package com.exampleepam.restaurant.service.forecast;

import com.exampleepam.restaurant.dto.forecast.DishForecastDto;
import com.exampleepam.restaurant.dto.forecast.IngredientForecastDto;
import com.exampleepam.restaurant.entity.*;
import com.exampleepam.restaurant.repository.DishRepository;
import com.exampleepam.restaurant.repository.IngredientForecastRepository;
import com.exampleepam.restaurant.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngredientForecastService {

    private final DishForecastService dishForecastService;
    private final DishRepository dishRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientForecastRepository forecastRepository;

    @Transactional(readOnly = true)
    public Page<IngredientForecastDto> getIngredientForecasts(
            int historyDays, String filter, Category type, String modelName, Pageable pageable
    ) {
        return buildIngredientForecasts(historyDays, filter, type, modelName, pageable, false);
    }

    @Transactional
    public Page<IngredientForecastDto> getIngredientForecasts(
            int historyDays, String filter, Category type, String modelName, Pageable pageable, boolean persist
    ) {
        return buildIngredientForecasts(historyDays, filter, type, modelName, pageable, persist);
    }

    private Page<IngredientForecastDto> buildIngredientForecasts(
            int historyDays, String filter, Category type, String modelName, Pageable pageable, boolean persist
    ) {
        // 1) Pull forecasted dishes
        Page<DishForecastDto> dishForecasts =
                dishForecastService.getDishForecasts(historyDays, null, type, modelName, Pageable.unpaged(), persist);

        if (dishForecasts.isEmpty()) {
            return page(List.of(), pageable);
        }

        Map<Long, Dish> dishMap = dishRepository.findAllActiveWithIngredients().stream()
                .collect(Collectors.toMap(Dish::getId, Function.identity()));

        Map<Long, IngredientForecastDto> agg = aggregate(dishForecasts.getContent(), dishMap, filter);

        List<IngredientForecastDto> list = new ArrayList<>(agg.values());
        list.sort(Comparator.comparing(IngredientForecastDto::getName, String.CASE_INSENSITIVE_ORDER));

        if (persist && !list.isEmpty()) {
            persistForecasts(list);
        }

        return page(list, pageable);
    }

    private Map<Long, IngredientForecastDto> aggregate(List<DishForecastDto> dishForecasts,
                                                       Map<Long, Dish> dishMap,
                                                       String filter) {
        String f = (filter == null || filter.isBlank()) ? null : filter.toLowerCase(Locale.ROOT);

        Map<Long, IngredientForecastDto> aggMap = new LinkedHashMap<>();

        for (DishForecastDto df : dishForecasts) {
            Dish dish = dishMap.get(df.getId());
            if (dish == null || dish.getIngredients() == null) continue;

            for (DishIngredient di : dish.getIngredients()) {
                Ingredient ing = di.getIngredient();
                if (ing == null) continue;

                String name = ing.getName();
                if (f != null && (name == null || !name.toLowerCase(Locale.ROOT).contains(f))) continue;

                long qty = di.getQuantity(); // assumes integer quantity; if decimal, switch to BigDecimal
                if (qty == 0) continue;

                IngredientForecastDto dto = aggMap.computeIfAbsent(
                        ing.getId(),
                        id -> new IngredientForecastDto(
                                id,
                                name,
                                ing.getUnit(),
                                new HashMap<>(),
                                new HashMap<>(),
                                new HashMap<>(),
                                true,   // emptyForecast starts TRUE; will become false if we add any forecast value
                                false,
                                false
                        )
                );

                mergeDishForecastIntoIngredient(dto, df, qty);
            }
        }

        // finalize flags
        for (IngredientForecastDto dto : aggMap.values()) {
            List<Integer> monthly = dto.getActualData().get("monthly");
            long nonZero = (monthly == null) ? 0 : monthly.stream().filter(v -> v != null && v > 0).count();
            dto.setNoData(nonZero == 0);
            dto.setSinglePoint(nonZero == 1);

            // don’t spam logs with full arrays
            log.trace("Ingredient {} nonZeroMonthlyPoints={}", dto.getName(), nonZero);
        }

        return aggMap;
    }

    private void mergeDishForecastIntoIngredient(IngredientForecastDto dto, DishForecastDto df, long qty) {
        Map<String, List<String>> labelsByScale = df.getLabels();
        if (labelsByScale == null) return;

        for (Map.Entry<String, List<String>> e : labelsByScale.entrySet()) {
            String scale = e.getKey();
            List<String> labels = e.getValue();
            if (labels == null) continue;

            dto.getLabels().putIfAbsent(scale, new ArrayList<>(labels));

            List<Integer> aList = safeGet(df.getActualData(), scale);
            List<Integer> fList = safeGet(df.getForecastData(), scale);
            if (aList == null && fList == null) continue;

            int len = maxSize(labels, aList, fList);
            if (len == 0) continue;

            List<Integer> aAgg = dto.getActualData().computeIfAbsent(scale, s -> new ArrayList<>(Collections.nCopies(len, null)));
            List<Integer> fAgg = dto.getForecastData().computeIfAbsent(scale, s -> new ArrayList<>(Collections.nCopies(len, null)));

            // if lists are shorter than labels, extend to len
            ensureSize(aAgg, len);
            ensureSize(fAgg, len);

            boolean addedAnyForecast = false;

            for (int i = 0; i < len; i++) {
                Integer aVal = (aList != null && i < aList.size()) ? aList.get(i) : null;
                Integer fVal = (fList != null && i < fList.size()) ? fList.get(i) : null;

                if (aVal != null) aAgg.set(i, safeAddMul(aAgg.get(i), aVal, qty));
                if (fVal != null) {
                    fAgg.set(i, safeAddMul(fAgg.get(i), fVal, qty));
                    addedAnyForecast = true;
                }
            }

            if (addedAnyForecast) {
                dto.setEmptyForecast(false);
            }
        }
    }

    private void persistForecasts(List<IngredientForecastDto> dtos) {
        LocalDate today = LocalDate.now();

        // one delete per run (add this method to repository)
        forecastRepository.deleteByGeneratedAt(today);

        Map<Long, Ingredient> ingredients = ingredientRepository.findAllById(
                dtos.stream().map(IngredientForecastDto::getId).toList()
        ).stream().collect(Collectors.toMap(Ingredient::getId, Function.identity()));

        Map<String, LocalDate> labelDateCache = new HashMap<>();
        List<IngredientForecast> batch = new ArrayList<>();

        for (IngredientForecastDto dto : dtos) {
            Ingredient ingredient = ingredients.get(dto.getId());
            if (ingredient == null) continue;

            List<Integer> monthly = dto.getForecastData().get("monthly");
            List<String> labels = dto.getLabels().get("monthly");
            if (monthly == null || labels == null) continue;

            int len = Math.min(monthly.size(), labels.size());
            for (int i = 0; i < len; i++) {
                Integer val = monthly.get(i);
                if (val == null) continue;

                LocalDate date = labelDateCache.computeIfAbsent(labels.get(i),
                        s -> YearMonth.parse(s).atDay(1));

                IngredientForecast entity = new IngredientForecast();
                entity.setIngredient(ingredient);
                entity.setDate(date);
                entity.setQuantity(val);
                entity.setGeneratedAt(today);
                batch.add(entity);
            }
        }

        if (!batch.isEmpty()) {
            forecastRepository.saveAll(batch);
        }
        log.debug("Persisted {} ingredient forecast rows", batch.size());
    }

    public List<IngredientForecast> getDetails(long ingredientId) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId).orElse(null);
        if (ingredient == null) return List.of();
        return forecastRepository.findByIngredientAndDateAfter(ingredient, LocalDate.now().minusYears(1));
    }

    private static Page<IngredientForecastDto> page(List<IngredientForecastDto> list, Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return new PageImpl<>(list);
        }
        int start = (int) Math.min(pageable.getOffset(), list.size());
        int end = Math.min(start + pageable.getPageSize(), list.size());
        return new PageImpl<>(list.subList(start, end), pageable, list.size());
    }

    private static <T> List<T> safeGet(Map<String, List<T>> map, String key) {
        return map == null ? null : map.get(key);
    }

    private static int maxSize(List<?> labels, List<?> a, List<?> f) {
        int l = labels == null ? 0 : labels.size();
        int aa = a == null ? 0 : a.size();
        int ff = f == null ? 0 : f.size();
        return Math.max(l, Math.max(aa, ff));
    }

    private static void ensureSize(List<Integer> list, int size) {
        while (list.size() < size) list.add(null);
    }

    private static Integer safeAddMul(Integer current, int value, long qty) {
        long cur = current == null ? 0L : current.longValue();
        long res = cur + (long) value * qty;

        // clamp to int range (or change DTO type to Long)
        if (res > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (res < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) res;
    }
}
