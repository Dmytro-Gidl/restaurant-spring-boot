package com.exampleepam.restaurant.service;

import com.exampleepam.restaurant.dto.dish.DishCreationDto;
import com.exampleepam.restaurant.dto.dish.DishResponseDto;
import com.exampleepam.restaurant.entity.Category;
import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.entity.paging.Paged;
import com.exampleepam.restaurant.entity.paging.Paging;
import com.exampleepam.restaurant.mapper.DishMapper;
import com.exampleepam.restaurant.repository.DishRepository;
import com.exampleepam.restaurant.repository.ReviewRepository;
import com.exampleepam.restaurant.util.FileUploadUtil;
import com.exampleepam.restaurant.util.FolderDeleteUtil;
import com.exampleepam.restaurant.util.ServiceUtil;
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

/**
 * Service for the Dish entity
 */
@Service
public class DishService {
    private final DishRepository dishRepository;
    private final DishMapper dishMapper;
    private final ServiceUtil serviceUtil;
    private final ReviewRepository reviewRepository;
    private static final String CATEGORY_ALL = "all";

    @Autowired
    public DishService(DishRepository dishRepository, DishMapper dishMapper, ServiceUtil serviceUtil,
                       ReviewRepository reviewRepository) {
        this.dishRepository = dishRepository;
        this.dishMapper = dishMapper;
        this.serviceUtil = serviceUtil;
        this.reviewRepository = reviewRepository;
    }

    /**
     * Returns a Paged object with a list of sorted dishes filtered by category
     *
     * @param currentPage current page
     * @param pageSize    number of rows per page
     * @param sortField   sort column for rows
     * @param sortDir     sort direction for rows
     * @param category    filter category
     * @return a Paged object with a sorted and filtered by category list of DishResponseDTOs
     * or an empty list if nothing is found
     */
    public Paged<DishResponseDto> findPaginated(int currentPage, int pageSize, String sortField,
                                                String sortDir, String category) {


        Sort sort = serviceUtil.getSort(sortField, sortDir);
        Pageable pageable = PageRequest.of(currentPage - 1, pageSize, sort);

        Page<Dish> dishPage;

        if (category.equals(CATEGORY_ALL)) {
            dishPage = dishRepository.findAll(pageable);
        } else {
            dishPage = dishRepository
                    .findPagedByCategory(Category.valueOf(category.toUpperCase(Locale.ENGLISH)), pageable);
        }

        Page<DishResponseDto> dishResponseDtoPage = dishPage
                .map(dishMapper::toDishResponseDto)
                .map(dto -> {
                    setAverageRating(dto);
                    return dto;
                });

        return new Paged<>(dishResponseDtoPage, Paging.of(dishPage.getTotalPages(), currentPage, pageSize));
    }

    /**
     * Saves a Dish
     *
     * @param dishCreationDto dish to be saved
     * @return persisted id
     */
    public long save(DishCreationDto dishCreationDto) {
        Dish dish = dishMapper.toDish(dishCreationDto);
        return dishRepository.save(dish).getId();
    }

    /**
     * Saves a Dish with an image
     *
     * @param dishCreationDto dish to be saved
     * @param multipartFile   image to be saved
     * @return persisted dish id
     */
    public long saveWithFile(DishCreationDto dishCreationDto, MultipartFile multipartFile) {
        Dish dish = dishMapper.toDish(dishCreationDto);
        String fileName = dish.getImageFileName();
        long persistedDishId = dishRepository.save(dish).getId();
        String uploadDir = "dish-images/" + persistedDishId;
        try {
            FolderDeleteUtil.deleteDishFolder(persistedDishId);
            FileUploadUtil.saveFile(uploadDir, fileName, multipartFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return persistedDishId;

    }

    /**
     * Returns a DishResponseDto fetched by id
     *
     * @param id Dish id
     * @return persisted id
     */
    public DishResponseDto getDishById(long id) {
        Dish dish = dishRepository.getById(id);
        DishResponseDto dto = dishMapper.toDishResponseDto(dish);
        setAverageRating(dto);
        return dto;
    }

    /**
     * Returns a sorted list of DishResponseDTOs filtered by category
     *
     * @param sortField sort column for rows
     * @param sortDir   sort direction for rows
     * @param category  filter category
     * @return a list of DishResponseDTOs
     */
    public List<DishResponseDto> findDishesByCategorySorted(String sortField,
                                                            String sortDir, String category) {
        Sort sort = serviceUtil.getSort(sortField, sortDir);
        List<Dish> dishes = dishRepository.findDishesByCategory(
                Category.valueOf(category.toUpperCase(Locale.ENGLISH)), sort);
        List<DishResponseDto> result = dishMapper.toDishResponseDtoList(dishes);
        assignAverageRatings(result);
        return result;
    }

    /**
     * Returns a sorted list of DishResponseDTOs
     *
     * @param sortField sort column for rows
     * @param sortDir   sort direction for rows
     * @return a list of DishResponseDTOs
     */
    public List<DishResponseDto> findAllDishesSorted(String sortField, String sortDir) {
        Sort sort = serviceUtil.getSort(sortField, sortDir);
        List<Dish> dishes = dishRepository.findAll(sort);
        List<DishResponseDto> result = dishMapper.toDishResponseDtoList(dishes);
        assignAverageRatings(result);
        return result;
    }

    private void assignAverageRatings(List<DishResponseDto> dishes) {
        dishes.forEach(this::setAverageRating);
    }

    private void setAverageRating(DishResponseDto dto) {
        Double avg = reviewRepository.getAverageRatingByDishId(dto.getId());
        dto.setAverageRating(avg == null ? 0 : avg);
    }

    /**
     * Deletes a Dish by id
     *
     * @param id id of the Dish to be deleted
     */
    public void deleteDishById(long id) {
        dishRepository.deleteById(id);
        FolderDeleteUtil.deleteDishFolder(id);
    }
}
