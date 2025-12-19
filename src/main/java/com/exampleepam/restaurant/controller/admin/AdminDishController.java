package com.exampleepam.restaurant.controller.admin;

import com.exampleepam.restaurant.controller.BaseController;
import com.exampleepam.restaurant.dto.dish.CategoryDto;
import com.exampleepam.restaurant.dto.dish.DishCreationDto;
import com.exampleepam.restaurant.dto.dish.DishResponseDto;
import com.exampleepam.restaurant.entity.paging.Paged;
import com.exampleepam.restaurant.security.AuthenticatedUser;
import com.exampleepam.restaurant.service.DishService;
import java.util.*;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Dish Controller for Admins
 */
@Controller
@Slf4j
@RequestMapping("/admin/dishes")
public class AdminDishController extends BaseController {

    private static final String DISH_PAGED = "dishPaged";
    private static final String DISH_ATTR = "dish";
    private static final String CATEGORIES_ATTR = "categories";

    private static final String REDIRECT_BASE = "redirect:/admin/dishes";
    private static final String PAGE_DISH_UPDATE = "dish-update";
    private static final String PAGE_DISH_ADD = "dish-add";
    private static final String PAGE_DISHES_MGMT = "dishes-management";

    private static final String PARAM_IMAGES = "images";
    private static final String PARAM_PRIMARY_INDEX = "primaryIndex";
    private static final String PARAM_EXISTING_IMAGES = "existingImages";
    private static final String PARAM_UPDATE_IMAGES = "updateImages";
    private static final String PARAM_DELETE_IMAGES = "deleteImages";

    private static final int DEFAULT_PAGE = 1;
    private static final String DEFAULT_SORT_FIELD = "category";
    private static final String DEFAULT_FILTER_CATEGORY = "all";
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final DishService dishService;

    @Autowired
    public AdminDishController(DishService dishService) {
        this.dishService = dishService;
    }

    @GetMapping("")
    public String getDishDefault(@AuthenticationPrincipal AuthenticatedUser user, Model model) {
        return findPaginated(
                DEFAULT_PAGE,
                DEFAULT_SORT_FIELD,
                ASCENDING_ORDER_SORTING,
                DEFAULT_FILTER_CATEGORY,
                DEFAULT_PAGE_SIZE,
                user,
                model
        );
    }

    @GetMapping("/page/{pageNo}")
    public String findPaginated(@PathVariable(PAGE_NUMBER_PARAM) int pageNo,
                                @RequestParam(value = SORT_FIELD_PARAM, defaultValue = DEFAULT_SORT_FIELD) String sortField,
                                @RequestParam(value = SORT_DIR_PARAM, defaultValue = ASCENDING_ORDER_SORTING) String sortDir,
                                @RequestParam(value = FILTER_CATEGORY_PARAM, defaultValue = DEFAULT_FILTER_CATEGORY) String filterCategory,
                                @RequestParam(value = PAGE_SIZE_PARAM, defaultValue = "" + DEFAULT_PAGE_SIZE) int pageSize,
                                @AuthenticationPrincipal AuthenticatedUser user,
                                Model model) {

        final String normalizedCategory = normalizeCategory(filterCategory);

        Paged<DishResponseDto> paged = dishService.findPaginated(
                pageNo, pageSize, sortField, sortDir, normalizedCategory
        );

        model.addAttribute(FILTER_CATEGORY_PARAM, normalizedCategory);
        model.addAttribute(CURRENT_PAGE_PARAM, pageNo);
        model.addAttribute(SORT_FIELD_PARAM, sortField);
        model.addAttribute(PAGE_SIZE_PARAM, pageSize);
        model.addAttribute(SORT_DIR_PARAM, sortDir);
        model.addAttribute(REVERSE_SORT_DIR_PARAM,
                ASCENDING_ORDER_SORTING.equals(sortDir) ? DESCENDING_ORDER_SORTING : ASCENDING_ORDER_SORTING);

        model.addAttribute(DISH_PAGED, paged);

        return PAGE_DISHES_MGMT;
    }

    @GetMapping("/newDishForm")
    public String returnDishCreationForm(Model model) {
        model.addAttribute(DISH_ATTR, new DishCreationDto());
        model.addAttribute(CATEGORIES_ATTR, CategoryDto.values());
        return PAGE_DISH_ADD;
    }

    @PostMapping
    public String saveNewDish(@Valid @ModelAttribute(DISH_ATTR) DishCreationDto dto,
                              BindingResult bindingResult,
                              @RequestParam(PARAM_IMAGES) List<MultipartFile> files,
                              @RequestParam(value = PARAM_PRIMARY_INDEX, defaultValue = "0") int primaryIndex,
                              Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(DISH_ATTR, dto);
            model.addAttribute(CATEGORIES_ATTR, CategoryDto.values());
            return PAGE_DISH_ADD;
        }

        final List<MultipartFile> nonEmpty = normalizeFiles(files);
        if (!nonEmpty.isEmpty()) {
            final List<String> fileNames = toSafeFileNames(nonEmpty);
            final int idx = clampIndex(primaryIndex, fileNames.size());
            dto.setImageFileName(fileNames.get(idx));
            dto.setGalleryImageFileNames(fileNames);
            dishService.saveWithFiles(dto, nonEmpty);
        } else {
            dishService.save(dto);
        }
        return REDIRECT_BASE;
    }

    @DeleteMapping("/{id}/page/{page}")
    public String deleteDish(@PathVariable("id") long id,
                             @PathVariable("page") int pageNo,
                             @RequestParam(SORT_FIELD_PARAM) String sortField,
                             @RequestParam(SORT_DIR_PARAM) String sortDir,
                             @RequestParam(PAGE_SIZE_PARAM) int pageSize,
                             @RequestParam(FILTER_CATEGORY_PARAM) String filterCategory) {
        dishService.deleteDishById(id);
        return "redirect:" + buildRedirect(pageNo, sortField, sortDir, pageSize, filterCategory);
    }

    @PutMapping("/{id}/restore/page/{page}")
    public String restoreDish(@PathVariable("id") long id,
                              @PathVariable("page") int pageNo,
                              @RequestParam(SORT_FIELD_PARAM) String sortField,
                              @RequestParam(SORT_DIR_PARAM) String sortDir,
                              @RequestParam(PAGE_SIZE_PARAM) int pageSize,
                              @RequestParam(FILTER_CATEGORY_PARAM) String filterCategory) {
        dishService.restoreDishById(id);
        return "redirect:" + buildRedirect(pageNo, sortField, sortDir, pageSize, filterCategory);
    }

    @DeleteMapping("/{id}/hard-delete/page/{page}")
    public String hardDeleteDish(@PathVariable("id") long id,
                                 @PathVariable("page") int pageNo,
                                 @RequestParam(SORT_FIELD_PARAM) String sortField,
                                 @RequestParam(SORT_DIR_PARAM) String sortDir,
                                 @RequestParam(PAGE_SIZE_PARAM) int pageSize,
                                 @RequestParam(FILTER_CATEGORY_PARAM) String filterCategory) {
        dishService.hardDeleteDish(id);
        return "redirect:" + buildRedirect(pageNo, sortField, sortDir, pageSize, filterCategory);
    }

    @PutMapping("/{id}")
    public String updateDish(@Valid @ModelAttribute(DISH_ATTR) DishCreationDto dto,
                             BindingResult bindingResult,
                             @RequestParam(value = PARAM_IMAGES, required = false) List<MultipartFile> newImages,
                             @RequestParam(value = PARAM_EXISTING_IMAGES, required = false) List<String> existingImages,
                             @RequestParam(value = PARAM_UPDATE_IMAGES, required = false) List<MultipartFile> updateImages,
                             @RequestParam(value = PARAM_DELETE_IMAGES, required = false) List<String> deleteImages,
                             @RequestParam(value = PARAM_PRIMARY_INDEX, defaultValue = "0") int primaryIndex,
                             Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute(DISH_ATTR, dto);
            model.addAttribute(CATEGORIES_ATTR, CategoryDto.values());
            return PAGE_DISH_UPDATE;
        }

        final List<MultipartFile> newImagesNorm = normalizeFiles(newImages);
        final List<String> existing = new ArrayList<>(Optional.ofNullable(existingImages).orElseGet(List::of));
        final List<MultipartFile> updates = Optional.ofNullable(updateImages).orElseGet(List::of);
        final List<String> deletions = Optional.ofNullable(deleteImages).orElseGet(List::of);

        // drop deletions from existing
        existing.removeAll(deletions);

        // compute replacements (position-based)
        final Map<String, MultipartFile> replacements = new HashMap<>();
        for (int i = 0; i < Math.min(existing.size(), updates.size()); i++) {
            MultipartFile f = updates.get(i);
            if (f != null && !f.isEmpty()) {
                replacements.put(existing.get(i), f);
            }
        }

        // assemble all names to determine primary + gallery
        final List<String> newNames = toSafeFileNames(newImagesNorm);
        final List<String> allNames = new ArrayList<>(existing);
        allNames.addAll(newNames);

        if (allNames.isEmpty()) {
            dto.setImageFileName(null);
            dto.setGalleryImageFileNames(List.of());
        } else {
            final int idx = clampIndex(primaryIndex, allNames.size());
            final String primaryName = allNames.get(idx);
            dto.setImageFileName(primaryName);

            final List<String> gallery = new ArrayList<>(allNames);
            gallery.remove(primaryName);
            dto.setGalleryImageFileNames(gallery);
        }

        dishService.updateWithFiles(dto, newImagesNorm, replacements, deletions);
        return REDIRECT_BASE;
    }

    @GetMapping("{id}/update-form")
    public String returnDishUpdateForm(@PathVariable("id") long id, Model model) {
        DishResponseDto dto = dishService.getDishById(id);
        if (dto == null) {
            log.debug("Admin tried to update a dish with id {} but it was not found", id);
            return "redirect:/admin/orders";
        }
        model.addAttribute(DISH_ATTR, dto);
        model.addAttribute(CATEGORIES_ATTR, CategoryDto.values());
        return PAGE_DISH_UPDATE;
    }

    // ----------------- helpers -----------------

    private static String normalizeCategory(String category) {
        if (category == null) return DEFAULT_FILTER_CATEGORY;
        return category.replace("\"", "").trim().toLowerCase();
    }

    private static List<MultipartFile> normalizeFiles(List<MultipartFile> files) {
        if (files == null) return new ArrayList<>();
        return files.stream()
                .filter(Objects::nonNull)
                .filter(f -> !f.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<String> toSafeFileNames(List<MultipartFile> files) {
        List<String> names = new ArrayList<>(files.size());
        for (MultipartFile f : files) {
            String name = StringUtils.cleanPath(Objects.requireNonNullElse(f.getOriginalFilename(), ""));
            if (!name.isBlank()) names.add(name);
        }
        return names;
    }

    private static int clampIndex(int idx, int size) {
        if (size <= 0) return 0;
        if (idx < 0) return 0;
        if (idx >= size) return size - 1;
        return idx;
    }

    private static String buildRedirect(int pageNo, String sortField, String sortDir, int pageSize, String filterCategory) {
        return UriComponentsBuilder.fromPath("/admin/dishes/page/{pageNo}")
                .queryParam(SORT_FIELD_PARAM, sortField)
                .queryParam(SORT_DIR_PARAM, sortDir)
                .queryParam(PAGE_SIZE_PARAM, pageSize)
                .queryParam(FILTER_CATEGORY_PARAM, filterCategory)
                .buildAndExpand(pageNo)
                .toUriString();
    }
}
