package com.exampleepam.restaurant.controller.admin;

import com.exampleepam.restaurant.dto.DishCreationDto;
import com.exampleepam.restaurant.dto.DishResponseDto;
import com.exampleepam.restaurant.entity.paging.Paged;
import com.exampleepam.restaurant.security.AuthenticatedUser;
import com.exampleepam.restaurant.service.DishService;
import com.exampleepam.restaurant.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;

@Controller
@Slf4j
@RequestMapping("/admin/dishes")
public class AdminDishController {
    private final DishService dishService;
    private final UserService userService;

    @Autowired
    public AdminDishController(DishService dishService, UserService userService) {
        this.dishService = dishService;
        this.userService = userService;
    }


    @GetMapping
    public String getDishDefault(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                 Model model) {
        return findPaginated(1,
                "category", "asc", "all", 10, authenticatedUser, model);
    }

    @GetMapping("/page/{pageNo}")
    public String findPaginated(@PathVariable("pageNo") int pageNo,
                                @RequestParam("sortField") String sortField,
                                @RequestParam("sortDir") String sortDir,
                                @RequestParam("filterCategory") String filterCategory,
                                @RequestParam("pageSize") int pageSize,
                                @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                Model model) {
        Paged<DishResponseDto> pagedOrder = dishService.findPaginated(pageNo, pageSize,
                sortField, sortDir, filterCategory);

        model.addAttribute("filterCategory", filterCategory);
        model.addAttribute("currentPage", pageNo);

        model.addAttribute("sortField", sortField);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");

        model.addAttribute("dishPaged", pagedOrder);


        long userId = authenticatedUser.getUserId();
        model.addAttribute("userBalance", userService.getUserBalance(userId));

        return "dishes-management";
    }

    @GetMapping("/newDishForm")
    public String returnDishCreationForm(Model model) {
        model.addAttribute("dish", new DishCreationDto());
        return "dish-add";
    }


    @PostMapping
    public String saveNewDish(@Valid @ModelAttribute("dish") DishCreationDto dishCreationDto,
                              BindingResult bindingResult,
                              @RequestParam("image") MultipartFile multipartFile,
                              Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("dish", dishCreationDto);
            return "dish-add";
        }
        if (!multipartFile.isEmpty()) {
            saveDishWithFile(dishCreationDto, multipartFile);
        } else {
            dishService.saveDish(dishCreationDto);
        }

        return "redirect:/admin/dishes";
    }

    private void saveDishWithFile(DishCreationDto dishCreationDto, MultipartFile multipartFile) {
        String fileName = StringUtils.cleanPath(multipartFile.getOriginalFilename());
        dishCreationDto.setImageFileName(fileName);
        long persistedDishId = dishService.saveDish(dishCreationDto);
        dishService.saveImage(multipartFile, persistedDishId, fileName);
    }

    @DeleteMapping("/{id}/page/{page}")
    public String deleteDish(
            @PathVariable(value = "id") int id,
            @PathVariable(value = "page") int pageNo,
            @RequestParam("sortField") String sortField,
            @RequestParam("sortDir") String sortDir,
            @RequestParam("pageSize") int pageSize,
            @RequestParam("filterCategory") String filterCategory
    ) {
        try {
            dishService.deleteDishById(id);
        } catch (EntityNotFoundException e) {
            log.warn(String.format("Admin tried to delete dish with id %d. But the Dish was not found in DB", id), e);
        }

        String redirectLink = UriComponentsBuilder.fromPath("/admin/dishes/page/{pageNo}")
                .queryParam("sortField", sortField)
                .queryParam("sortDir", sortDir)
                .queryParam("pageSize", pageSize)
                .queryParam("filterCategory", filterCategory)
                .buildAndExpand(pageNo)
                .toUriString();
        return "redirect:" + redirectLink;
    }

    @PutMapping("/{id}")
    public String updateDish(
            @Valid @ModelAttribute("dish") DishCreationDto dishCreationDto,
            BindingResult bindingResult,
            @RequestParam(value = "image", required = false) MultipartFile multipartFile,
            Model model
    ) {
        long dishId = dishCreationDto.getId();

        if (bindingResult.hasErrors()) {

            model.addAttribute("dish", dishCreationDto);
            return "dish-update";
        }


        if (!multipartFile.isEmpty()) {
            saveDishWithFile(dishCreationDto, multipartFile);
        } else {
            try {
                var oldDish = dishService.getDishById(dishId);

                dishCreationDto.setImageFileName(oldDish.getImageFileName());
                dishService.saveDish(dishCreationDto);
            } catch (EntityNotFoundException e) {
                log.warn(String.format("Admin tried to update dish with ud %d. But the dish was not found in DB",
                        dishId), e);
            }
        }

        return "redirect:/admin/dishes";
    }


    @GetMapping("{id}/update-form")
    public String returnDishUpdateForm(
            @PathVariable(value = "id") long id,
            Model model) {
        try {
                DishResponseDto dishResponseDto = dishService.getDishById(id);
            model.addAttribute("dish", dishResponseDto);
        } catch (EntityNotFoundException e) {
            log.warn(String.format("Admin tried to update dish with ud %d. But the dish was not found in DB", id), e);
        }

        return "dish-update";
    }
}
