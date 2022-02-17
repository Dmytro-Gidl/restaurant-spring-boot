package com.exampleepam.restaurant.service;

import com.exampleepam.restaurant.dto.DishCreationDto;
import com.exampleepam.restaurant.dto.DishResponseDto;
import com.exampleepam.restaurant.entity.Category;
import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.entity.paging.Paged;
import com.exampleepam.restaurant.entity.paging.Paging;
import com.exampleepam.restaurant.exception.EntityType;
import com.exampleepam.restaurant.mapper.DishMapper;
import com.exampleepam.restaurant.repository.DishRepository;
import com.exampleepam.restaurant.util.FileUploadUtil;
import com.exampleepam.restaurant.util.FolderDeleteUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.exampleepam.restaurant.exception.ExceptionManager.getNotFoundException;

@Service
public class DishService {
    DishRepository dishRepository;
    DishMapper dishMapper;
    private static final String CATEGORY_ALL = "all";

    @Autowired
    public DishService(DishRepository dishRepository, DishMapper dishMapper) {
        this.dishRepository = dishRepository;
        this.dishMapper = dishMapper;
    }

    public Paged<DishResponseDto> findPaginated(int pageNo, int pageSize, String sortField,
                                                String sortDirection, String category) {

        Sort primarySort = sortDirection.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Sort secondarySort = Sort.by("id").ascending();
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize, primarySort.and(secondarySort));

        Page<Dish> dishPage;

        if (category.equals(CATEGORY_ALL)) {
            dishPage = dishRepository.findAll(pageable);
        } else {
            dishPage = dishRepository
                    .findPagedByCategory(Category.valueOf(category.toUpperCase(Locale.ENGLISH)), pageable);
        }

        Page<DishResponseDto> dishResponseDtoPage = dishPage
                .map(dish -> dishMapper.toDishResponseDto(dish));
        return new Paged<>(dishResponseDtoPage, Paging.of(dishPage.getTotalPages(), pageNo, pageSize));

    }

    public void saveImage(MultipartFile multipartFile, long persistedDishId, String fileName) {

        String uploadDir = "dish-images/" + persistedDishId;
        try {
            FolderDeleteUtil.deleteDishFolder(persistedDishId);
            FileUploadUtil.saveFile(uploadDir, fileName, multipartFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public long saveDish(DishCreationDto dishCreationDto) {
        Dish dish = dishMapper.toDish(dishCreationDto);
        return dishRepository.save(dish).getId();
    }

    public DishResponseDto getDishById(long id) {
        Dish dish = dishRepository.findById(id).orElseThrow(() ->
                getNotFoundException(EntityType.DISH, id));
        return dishMapper.toDishResponseDto(dish);
    }

    public List<DishResponseDto> findDishesByCategorySorted(String sortField,
                                                            String sortDirection, String category) {
        Sort sort = getSort(sortDirection, sortField);
        List<Dish> dishes = dishRepository.findDishesByCategory(
                Category.valueOf(category.toUpperCase(Locale.ENGLISH)), sort);
        return toDishResponseDtoList(dishes);
    }

    public List<DishResponseDto> findAllDishesSorted(String sortField, String sortDirection) {
        Sort sort = getSort(sortDirection, sortField);
        List<Dish> dishes = dishRepository.findAll(sort);
        return toDishResponseDtoList(dishes);
    }

    private List<DishResponseDto> toDishResponseDtoList(List<Dish> dishes) {
        return dishes.stream()
                .map(dish -> dishMapper.toDishResponseDto(dish))
                .collect(Collectors.toList());
    }


    public Sort getSort(String sortDirection, String sortField) {
        return sortDirection.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
    }

    public void deleteDishById(long id) {
        int delete = dishRepository.deleteDishById(id);
        if (delete == 0) {
            throw getNotFoundException(EntityType.DISH, id);
        }
        FolderDeleteUtil.deleteDishFolder(id);
    }
}
