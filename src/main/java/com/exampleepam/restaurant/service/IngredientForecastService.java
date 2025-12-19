package com.exampleepam.restaurant.service;

import com.exampleepam.restaurant.dto.forecast.DishForecastDto;
import com.exampleepam.restaurant.dto.forecast.IngredientForecastDto;
import com.exampleepam.restaurant.entity.Category;
import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.entity.DishIngredient;
import com.exampleepam.restaurant.entity.MeasureUnit;
import com.exampleepam.restaurant.entity.Ingredient;
import com.exampleepam.restaurant.entity.IngredientForecast;
import com.exampleepam.restaurant.repository.DishRepository;
import com.exampleepam.restaurant.repository.IngredientForecastRepository;
import com.exampleepam.restaurant.repository.IngredientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service producing aggregated forecasts for ingredients based on dish forecasts.
 */
@Service
public class IngredientForecastService {

    private final DishForecastService dishForecastService;
    private final DishRepository dishRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientForecastRepository forecastRepository;
    private static final Logger log = LoggerFactory.getLogger(IngredientForecastService.class);

    @Autowired
    public IngredientForecastService(DishForecastService dishForecastService,
                                     DishRepository dishRepository,
                                     IngredientRepository ingredientRepository,
                                     IngredientForecastRepository forecastRepository) {
        this.dishForecastService = dishForecastService;
        this.dishRepository = dishRepository;
        this.ingredientRepository = ingredientRepository;
        this.forecastRepository = forecastRepository;
    }

    /**
     * Build ingredient forecasts by aggregating dish forecasts. Each ingredient
     * contribution equals the forecasted dish demand multiplied by the amount
     * of that ingredient used in the dish.
     */
    @Transactional
    public Page<IngredientForecastDto> getIngredientForecasts(int historyDays, String filter, Category type,
                                                             String modelName, Pageable pageable) {
        return getIngredientForecasts(historyDays, filter, type, modelName, pageable, false);
    }

    @Transactional
    public Page<IngredientForecastDto> getIngredientForecasts(int historyDays, String filter, Category type,
                                                             String modelName, Pageable pageable, boolean persist) {
        Map<Long, Dish> dishMap = dishRepository.findAll().stream()
                .collect(Collectors.toMap(Dish::getId, d -> d));
        Page<DishForecastDto> dishForecasts = fetchDishForecasts(historyDays, type, modelName, persist);
        log.debug("Fetched {} dish forecasts for ingredients", dishForecasts.getContent().size());
        Map<Long, IngredientForecastDto> aggMap = aggregateIngredientData(dishForecasts, dishMap, filter);
        log.debug("Aggregated to {} ingredient entries", aggMap.size());
        if (persist) {
            persistForecasts(aggMap.values());
        }
        List<IngredientForecastDto> list = new ArrayList<>(aggMap.values());
        list.sort(Comparator.comparing(IngredientForecastDto::getName));
        if (pageable == null || pageable.isUnpaged()) {
            log.debug("Returning {} ingredient forecasts without pagination", list.size());
            return new PageImpl<>(list);
        }

        int start = (int) Math.min(pageable.getOffset(), list.size());
        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<IngredientForecastDto> content = list.subList(start, end);
        log.debug("Returning {} ingredient forecasts", content.size());
        return new PageImpl<>(content, pageable, list.size());
    }

    private Page<DishForecastDto> fetchDishForecasts(int historyDays, Category type, String modelName, boolean persist) {
        return dishForecastService.getDishForecasts(historyDays, null, type, modelName, Pageable.unpaged(), persist);
    }

    /**
     * Combine dish forecasts into ingredient-level projections and apply an
     * optional ingredient name filter.
     */
    private Map<Long, IngredientForecastDto> aggregateIngredientData(Page<DishForecastDto> dishForecasts,
                                                                     Map<Long, Dish> dishMap,
                                                                     String filter) {
        Map<Long, IngredientForecastDto> aggMap = new LinkedHashMap<>();
        String f = filter == null ? null : filter.toLowerCase();
        for (DishForecastDto df : dishForecasts.getContent()) {
            Dish dish = dishMap.get(df.getId());
            if (dish == null) {
                continue;
            }
            for (DishIngredient di : dish.getIngredients()) {
                String ingName = di.getIngredient().getName();
                if (f != null && !ingName.toLowerCase().contains(f)) {
                    continue;
                }
                MeasureUnit unit = di.getIngredient().getUnit();
                IngredientForecastDto dto = aggMap.computeIfAbsent(
                        di.getIngredient().getId(),
                        id -> new IngredientForecastDto(id, ingName, unit,
                                new HashMap<>(), new HashMap<>(), new HashMap<>(), false, false, false));
                for (String scale : df.getLabels().keySet()) {
                    dto.getLabels().putIfAbsent(scale, new ArrayList<>(df.getLabels().get(scale)));
                    List<Integer> aList = df.getActualData().get(scale);
                    List<Integer> fList = df.getForecastData().get(scale);
                    if (aList == null || fList == null) {
                        continue; // nothing to aggregate
                    }
                    List<Integer> aAgg = dto.getActualData().computeIfAbsent(scale,
                            s -> new ArrayList<>(Collections.nCopies(aList.size(), null)));
                    List<Integer> fAgg = dto.getForecastData().computeIfAbsent(scale,
                            s -> new ArrayList<>(Collections.nCopies(fList.size(), null)));
                    for (int i = 0; i < aList.size(); i++) {
                        Integer aVal = aList.get(i);
                        if (aVal != null) {
                            Integer curA = aAgg.get(i);
                            aAgg.set(i, (curA == null ? 0 : curA) + aVal * di.getQuantity());
                        }
                        Integer fVal = fList.get(i);
                        if (fVal != null) {
                            Integer curF = fAgg.get(i);
                            fAgg.set(i, (curF == null ? 0 : curF) + fVal * di.getQuantity());
                        }
                    }
                    dto.setEmptyForecast(dto.isEmptyForecast() || df.isEmptyForecast());
                }
            }
        }
        aggMap.values().forEach(dto -> {
            List<Integer> monthly = dto.getActualData().get("monthly");
            long nonZero = monthly == null ? 0 : monthly.stream().filter(v -> v != null && v > 0).count();
            dto.setNoData(nonZero == 0);
            dto.setSinglePoint(nonZero == 1);
            log.debug("Ingredient {} monthly totals {}", dto.getName(), monthly);
        });
        return aggMap;
    }

    private void persistForecasts(Collection<IngredientForecastDto> dtos) {
        forecastRepository.deleteByGeneratedAtBefore(java.time.LocalDate.now());
        int saved = 0;
        for (IngredientForecastDto dto : dtos) {
            Ingredient ingredient = ingredientRepository.findById(dto.getId()).orElse(null);
            if (ingredient == null) continue;
            forecastRepository.deleteByIngredientAndGeneratedAt(ingredient, java.time.LocalDate.now());
            List<Integer> monthly = dto.getForecastData().get("monthly");
            List<String> labels = dto.getLabels().get("monthly");
            if (monthly == null || labels == null) continue;
            for (int i = 0; i < monthly.size(); i++) {
                Integer val = monthly.get(i);
                if (val == null) continue;
                IngredientForecast entity = new IngredientForecast();
                entity.setIngredient(ingredient);
                entity.setDate(java.time.YearMonth.parse(labels.get(i)).atDay(1));
                entity.setQuantity(val);
                entity.setGeneratedAt(java.time.LocalDate.now());
                forecastRepository.save(entity);
                saved++;
            }
        }
        log.debug("Persisted {} ingredient forecast rows", saved);
    }

    public java.util.List<IngredientForecast> getDetails(long ingredientId) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId).orElse(null);
        if (ingredient == null) return java.util.List.of();
        return forecastRepository.findByIngredientAndDateAfter(ingredient, java.time.LocalDate.now().minusYears(1));
    }
}

