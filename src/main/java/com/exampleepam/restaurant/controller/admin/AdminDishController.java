package com.exampleepam.restaurant.controller.admin;

import com.exampleepam.restaurant.controller.BaseController;
import com.exampleepam.restaurant.dto.dish.CategoryDto;
import com.exampleepam.restaurant.dto.dish.DishCreationDto;
import com.exampleepam.restaurant.dto.dish.DishResponseDto;
import com.exampleepam.restaurant.entity.paging.Paged;
import com.exampleepam.restaurant.security.AuthenticatedUser;
import com.exampleepam.restaurant.service.DishService;
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

import javax.validation.Valid;

/**
 * Dish Controller for Admins
 */
@Controller
@Slf4j
@RequestMapping("/admin/dishes")
public class AdminDishController extends BaseController {

    private static final String DISH_PAGED_ATTRIBUTE_NAME = "dishPaged";
    private static final String DISH_ATTRIBUTE_NAME = "dish";
    private static final String REDIRECT_TO_ADMIN_DISHES = "redirect:/admin/dishes";
    private static final String DISH_UPDATE_PAGE = "dish-update";
    private static final String DISH_ADD_PAGE = "dish-add";
    private static final String IMAGE_PARAM = "images";
    private static final String PRIMARY_INDEX_PARAM = "primaryIndex";
    private static final String DISHES_MANAGEMENT_PAGE = "dishes-management";
    private static final int DEFAULT_PAGE = 1;
    private static final String DEFAULT_SORT_FIELD = "category";
    private static final String DEFAULT_FILTER_CATEGORY = "all";
    private static final int DEFAULT_PAGE_SIZE = 10;
    private final DishService dishService;

    @Autowired
    public AdminDishController(DishService dishService) {
        this.dishService = dishService;
    }

    @GetMapping(value = {"", "/{id}"})
    public String getDishDefault(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                 Model model) {
        return findPaginated(DEFAULT_PAGE,
                DEFAULT_SORT_FIELD, ASCENDING_ORDER_SORTING, DEFAULT_FILTER_CATEGORY, DEFAULT_PAGE_SIZE, authenticatedUser, model);
    }

    @GetMapping("/page/{pageNo}")
    public String findPaginated(@PathVariable(PAGE_NUMBER_PARAM) int pageNo,
                                @RequestParam(SORT_FIELD_PARAM) String sortField,
                                @RequestParam(SORT_DIR_PARAM) String sortDir,
                                @RequestParam(FILTER_CATEGORY_PARAM) String filterCategory,
                                @RequestParam(PAGE_SIZE_PARAM) int pageSize,
                                @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                Model model) {
        String normalizedCategory = filterCategory == null ? DEFAULT_FILTER_CATEGORY
                : filterCategory.replace("\"", "").toLowerCase();

        Paged<DishResponseDto> pagedOrder = dishService.findPaginated(pageNo, pageSize,
                sortField, sortDir, normalizedCategory);

        model.addAttribute(FILTER_CATEGORY_PARAM, normalizedCategory);
        model.addAttribute(CURRENT_PAGE_PARAM, pageNo);

        model.addAttribute(SORT_FIELD_PARAM, sortField);
        model.addAttribute(PAGE_SIZE_PARAM, pageSize);
        model.addAttribute(SORT_DIR_PARAM, sortDir);
        model.addAttribute(REVERSE_SORT_DIR_PARAM, sortDir.equals(ASCENDING_ORDER_SORTING) ? DESCENDING_ORDER_SORTING : ASCENDING_ORDER_SORTING);

        model.addAttribute(DISH_PAGED_ATTRIBUTE_NAME, pagedOrder);

        return DISHES_MANAGEMENT_PAGE;
    }

    @GetMapping("/newDishForm")
    public String returnDishCreationForm(Model model) {
        model.addAttribute(DISH_ATTRIBUTE_NAME, new DishCreationDto());
        model.addAttribute("categories", CategoryDto.values());
        return DISH_ADD_PAGE;
    }

    @PostMapping
    public String saveNewDish(@Valid @ModelAttribute(DISH_ATTRIBUTE_NAME) DishCreationDto dishCreationDto,
                              BindingResult bindingResult,
                              @RequestParam(IMAGE_PARAM) java.util.List<MultipartFile> multipartFiles,
                              @RequestParam(value = PRIMARY_INDEX_PARAM, defaultValue = "0") int primaryIndex,
                              Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(DISH_ATTRIBUTE_NAME, dishCreationDto);
            model.addAttribute("categories", CategoryDto.values());
            return DISH_ADD_PAGE;
        }

        multipartFiles.removeIf(MultipartFile::isEmpty);
        if (!multipartFiles.isEmpty()) {
            java.util.List<String> fileNames = new java.util.ArrayList<>();
            for (MultipartFile file : multipartFiles) {
                fileNames.add(StringUtils.cleanPath(file.getOriginalFilename()));
            }
            if (primaryIndex < 0 || primaryIndex >= fileNames.size()) {
                primaryIndex = 0;
            }
            dishCreationDto.setImageFileName(fileNames.get(primaryIndex));
            dishCreationDto.setGalleryImageFileNames(fileNames);
            dishService.saveWithFiles(dishCreationDto, multipartFiles);
        } else {
            dishService.save(dishCreationDto);
        }
        return REDIRECT_TO_ADMIN_DISHES;
    }


    @DeleteMapping("/{id}/page/{page}")
    public String deleteDish(
            @PathVariable(value = "id") int id,
            @PathVariable(value = "page") int pageNo,
            @RequestParam(SORT_FIELD_PARAM) String sortField,
            @RequestParam(SORT_DIR_PARAM) String sortDir,
            @RequestParam(PAGE_SIZE_PARAM) int pageSize,
            @RequestParam(FILTER_CATEGORY_PARAM) String filterCategory
    ) {
        dishService.deleteDishById(id);


        String redirectLink = UriComponentsBuilder.fromPath("/admin/dishes/page/{pageNo}")
                .queryParam(SORT_FIELD_PARAM, sortField)
                .queryParam(SORT_DIR_PARAM, sortDir)
                .queryParam(PAGE_SIZE_PARAM, pageSize)
                .queryParam(FILTER_CATEGORY_PARAM, filterCategory)
                .buildAndExpand(pageNo)
                .toUriString();
        return "redirect:" + redirectLink;
    }

    @PutMapping("/{id}/restore/page/{page}")
    public String restoreDish(
            @PathVariable("id") long id,
            @PathVariable("page") int pageNo,
            @RequestParam(SORT_FIELD_PARAM) String sortField,
            @RequestParam(SORT_DIR_PARAM) String sortDir,
            @RequestParam(PAGE_SIZE_PARAM) int pageSize,
            @RequestParam(FILTER_CATEGORY_PARAM) String filterCategory) {
        dishService.restoreDishById(id);

        String redirectLink = UriComponentsBuilder.fromPath("/admin/dishes/page/{pageNo}")
                .queryParam(SORT_FIELD_PARAM, sortField)
                .queryParam(SORT_DIR_PARAM, sortDir)
                .queryParam(PAGE_SIZE_PARAM, pageSize)
                .queryParam(FILTER_CATEGORY_PARAM, filterCategory)
                .buildAndExpand(pageNo)
                .toUriString();
        return "redirect:" + redirectLink;
    }

    @DeleteMapping("/{id}/hard-delete/page/{page}")
    public String hardDeleteDish(
            @PathVariable("id") long id,
            @PathVariable("page") int pageNo,
            @RequestParam(SORT_FIELD_PARAM) String sortField,
            @RequestParam(SORT_DIR_PARAM) String sortDir,
            @RequestParam(PAGE_SIZE_PARAM) int pageSize,
            @RequestParam(FILTER_CATEGORY_PARAM) String filterCategory) {
        dishService.hardDeleteDish(id);

        String redirectLink = UriComponentsBuilder.fromPath("/admin/dishes/page/{pageNo}")
                .queryParam(SORT_FIELD_PARAM, sortField)
                .queryParam(SORT_DIR_PARAM, sortDir)
                .queryParam(PAGE_SIZE_PARAM, pageSize)
                .queryParam(FILTER_CATEGORY_PARAM, filterCategory)
                .buildAndExpand(pageNo)
                .toUriString();
        return "redirect:" + redirectLink;
    }

    @PutMapping("/{id}")
    public String updateDish(
            @Valid @ModelAttribute(DISH_ATTRIBUTE_NAME) DishCreationDto dishCreationDto,
            BindingResult bindingResult,
            @RequestParam(value = IMAGE_PARAM, required = false) java.util.List<MultipartFile> multipartFiles,
            @RequestParam(value = "existingImages", required = false) java.util.List<String> existingImages,
            @RequestParam(value = "updateImages", required = false) java.util.List<MultipartFile> updateImages,
            @RequestParam(value = "deleteImages", required = false) java.util.List<String> deleteImages,
            @RequestParam(value = PRIMARY_INDEX_PARAM, defaultValue = "0") int primaryIndex,
            Model model
    ) {
        long dishId = dishCreationDto.getId();

        if (bindingResult.hasErrors()) {

            model.addAttribute(DISH_ATTRIBUTE_NAME, dishCreationDto);
            model.addAttribute("categories", CategoryDto.values());
            return DISH_UPDATE_PAGE;
        }

        if (multipartFiles != null) {
            multipartFiles.removeIf(MultipartFile::isEmpty);
        } else {
            multipartFiles = new java.util.ArrayList<>();
        }

        if (updateImages == null) {
            updateImages = new java.util.ArrayList<>();
        }

        java.util.List<String> originalExisting = existingImages == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(existingImages);

        if (existingImages == null) {
            existingImages = new java.util.ArrayList<>();
        }

        if (deleteImages != null) {
            existingImages.removeAll(deleteImages);
        }

        java.util.Map<String, MultipartFile> replacements = new java.util.HashMap<>();
        for (int i = 0; i < originalExisting.size() && i < updateImages.size(); i++) {
            MultipartFile file = updateImages.get(i);
            if (!file.isEmpty()) {
                replacements.put(originalExisting.get(i), file);
            }
        }

        java.util.List<String> newFileNames = new java.util.ArrayList<>();
        for (MultipartFile file : multipartFiles) {
            newFileNames.add(StringUtils.cleanPath(file.getOriginalFilename()));
        }

        java.util.List<String> allNames = new java.util.ArrayList<>(existingImages);
        allNames.addAll(newFileNames);

        if (allNames.isEmpty()) {
            dishCreationDto.setImageFileName(null);
            dishCreationDto.setGalleryImageFileNames(java.util.List.of());
        } else {
            if (primaryIndex < 0 || primaryIndex >= allNames.size()) {
                primaryIndex = 0;
            }
            String primaryName = allNames.get(primaryIndex);
            dishCreationDto.setImageFileName(primaryName);
            allNames.remove(primaryName);
            dishCreationDto.setGalleryImageFileNames(allNames);
        }

        dishService.updateWithFiles(dishCreationDto, multipartFiles, replacements, deleteImages);

        return REDIRECT_TO_ADMIN_DISHES;
    }

    @GetMapping("{id}/update-form")
    public String returnDishUpdateForm(
            @PathVariable(value = "id") long id,
            Model model) {
        DishResponseDto dishResponseDto = dishService.getDishById(id);

        if (dishResponseDto != null) {
            model.addAttribute(DISH_ATTRIBUTE_NAME, dishResponseDto);
        } else {
            log.debug("Admin tried to update a dish with id {}. But the dish was not found in DB", id);
            return "redirect:/admin/orders";
        }
        model.addAttribute("categories", CategoryDto.values());
        return DISH_UPDATE_PAGE;
    }
}
