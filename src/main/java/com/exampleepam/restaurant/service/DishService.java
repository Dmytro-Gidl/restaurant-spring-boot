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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Locale;

/**
 * Service for the Dish entity
 */
@Service
public class DishService {
    private static final String CACHE_DISH_PAGES = "dishPages";
    private static final String CACHE_DISH_BY_CATEGORY = "dishCategory";
    private static final String CACHE_DISH_ALL = "dishAll";
    private static final String CACHE_DISH_BY_ID = "dishById";
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
    @Cacheable(cacheNames = CACHE_DISH_PAGES,
            key = "T(java.util.Arrays).asList(#currentPage, #pageSize, #sortField, #sortDir, #category)")
    public Paged<DishResponseDto> findPaginated(int currentPage, int pageSize, String sortField,
                                                String sortDir, String category) {


        Sort sort = serviceUtil.getSort(sortField, sortDir);
        Pageable pageable = PageRequest.of(currentPage - 1, pageSize, sort);

        Page<Dish> dishPage;

        String cat = category == null ? CATEGORY_ALL : category.replace("\"", "");

        if (cat.equalsIgnoreCase("archived")) {
            dishPage = dishRepository.findAllByArchivedTrue(pageable);
        } else if (cat.equalsIgnoreCase(CATEGORY_ALL)) {
            dishPage = dishRepository.findAllByArchivedFalse(pageable);
        } else {
            dishPage = dishRepository
                    .findPagedByCategoryAndArchivedFalse(
                            Category.valueOf(cat.trim().toUpperCase(Locale.ENGLISH)), pageable);
        }

        Page<DishResponseDto> dishResponseDtoPage = dishPage
                .map(dishMapper::toDishResponseDto)
                .map(dto -> {
                    setAverageRating(dto);
                    setReviewCount(dto);
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
    @CacheEvict(cacheNames = {
            CACHE_DISH_PAGES,
            CACHE_DISH_BY_CATEGORY,
            CACHE_DISH_ALL,
            CACHE_DISH_BY_ID
    }, allEntries = true)
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
    @CacheEvict(cacheNames = {
            CACHE_DISH_PAGES,
            CACHE_DISH_BY_CATEGORY,
            CACHE_DISH_ALL,
            CACHE_DISH_BY_ID
    }, allEntries = true)
    public long saveWithFiles(DishCreationDto dishCreationDto, java.util.List<MultipartFile> multipartFiles) {
        Dish dish = dishMapper.toDish(dishCreationDto);
        long persistedDishId = dishRepository.save(dish).getId();
        String uploadDir = "dish-images/" + persistedDishId;
        try {
            FolderDeleteUtil.deleteDishFolder(persistedDishId);
            java.util.List<String> fileNames = dishCreationDto.getGalleryImageFileNames();
            for (int i = 0; i < multipartFiles.size(); i++) {
                MultipartFile file = multipartFiles.get(i);
                if (file.isEmpty()) continue;
                String fileName = fileNames.get(i);
                FileUploadUtil.saveFile(uploadDir, fileName, file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return persistedDishId;

    }

    /**
     * Updates existing dish and processes image additions/removals.
     */
    @CacheEvict(cacheNames = {
            CACHE_DISH_PAGES,
            CACHE_DISH_BY_CATEGORY,
            CACHE_DISH_ALL,
            CACHE_DISH_BY_ID
    }, allEntries = true)
    public void updateWithFiles(DishCreationDto dto, java.util.List<MultipartFile> newFiles,
                                java.util.Map<String, MultipartFile> replaceFiles,
                                java.util.List<String> deleteFileNames) {
        Dish dish = dishMapper.toDish(dto);
        dishRepository.save(dish);
        String uploadDir = "dish-images/" + dish.getId();
        try {
            if (deleteFileNames != null) {
                for (String name : deleteFileNames) {
                    java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(uploadDir).resolve(name));
                }
            }
            if (replaceFiles != null) {
                for (var entry : replaceFiles.entrySet()) {
                    MultipartFile file = entry.getValue();
                    if (file.isEmpty()) continue;
                    FileUploadUtil.saveFile(uploadDir, entry.getKey(), file);
                }
            }
            if (newFiles != null) {
                for (MultipartFile file : newFiles) {
                    if (file.isEmpty()) continue;
                    String fileName = org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());
                    FileUploadUtil.saveFile(uploadDir, fileName, file);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Legacy method for backward compatibility when only one file was supported.
     */
    @CacheEvict(cacheNames = {
            CACHE_DISH_PAGES,
            CACHE_DISH_BY_CATEGORY,
            CACHE_DISH_ALL,
            CACHE_DISH_BY_ID
    }, allEntries = true)
    public long saveWithFile(DishCreationDto dto, MultipartFile file) {
        java.util.List<MultipartFile> list = new java.util.ArrayList<>();
        list.add(file);
        dto.setGalleryImageFileNames(java.util.List.of(dto.getImageFileName()));
        return saveWithFiles(dto, list);
    }

    /**
     * Returns a DishResponseDto fetched by id
     *
     * @param id Dish id
     * @return persisted id
     */
    @Cacheable(cacheNames = CACHE_DISH_BY_ID, key = "#id")
    public DishResponseDto getDishById(long id) {
        Dish dish = dishRepository.getById(id);
        DishResponseDto dto = dishMapper.toDishResponseDto(dish);
        setAverageRating(dto);
        setReviewCount(dto);
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
    @Cacheable(cacheNames = CACHE_DISH_BY_CATEGORY,
            key = "T(java.util.Arrays).asList(#sortField, #sortDir, #category)")
    public List<DishResponseDto> findDishesByCategorySorted(String sortField,
                                                            String sortDir, String category) {
        Sort sort = serviceUtil.getSort(sortField, sortDir);
        List<Dish> dishes = dishRepository.findDishesByCategoryAndArchivedFalse(
                Category.valueOf(category.toUpperCase(Locale.ENGLISH)), sort);
        List<DishResponseDto> result = dishMapper.toDishResponseDtoList(dishes);
        assignAverageRatings(result);
        assignReviewCounts(result);
        return result;
    }

    /**
     * Returns a sorted list of DishResponseDTOs
     *
     * @param sortField sort column for rows
     * @param sortDir   sort direction for rows
     * @return a list of DishResponseDTOs
     */
    @Cacheable(cacheNames = CACHE_DISH_ALL,
            key = "T(java.util.Arrays).asList(#sortField, #sortDir)")
    public List<DishResponseDto> findAllDishesSorted(String sortField, String sortDir) {
        Sort sort = serviceUtil.getSort(sortField, sortDir);
        List<Dish> dishes = dishRepository.findAllByArchivedFalse(sort);
        List<DishResponseDto> result = dishMapper.toDishResponseDtoList(dishes);
        assignAverageRatings(result);
        assignReviewCounts(result);
        return result;
    }

    private void assignAverageRatings(List<DishResponseDto> dishes) {
        dishes.forEach(this::setAverageRating);
    }

    private void assignReviewCounts(List<DishResponseDto> dishes) {
        dishes.forEach(this::setReviewCount);
    }

    private void setAverageRating(DishResponseDto dto) {
        Double avg = reviewRepository.getAverageRatingByDishId(dto.getId());
        dto.setAverageRating(avg == null ? 0 : avg);
    }

    private void setReviewCount(DishResponseDto dto) {
        Long count = reviewRepository.countByDishId(dto.getId());
        dto.setReviewCount(count == null ? 0 : count);
    }

    /**
     * Archive a Dish instead of deleting it. The dish images and reviews remain
     * intact, but it will no longer be shown on the public menu.
     *
     * @param id id of the Dish to be archived
     */
    @CacheEvict(cacheNames = {
            CACHE_DISH_PAGES,
            CACHE_DISH_BY_CATEGORY,
            CACHE_DISH_ALL,
            CACHE_DISH_BY_ID
    }, allEntries = true)
    public void archiveDishById(long id) {
        dishRepository.findById(id).ifPresent(dish -> {
            dish.setArchived(true);
            dishRepository.save(dish);
        });
    }

    /**
     * Alias for archiveDishById used by tests.
     */
    @CacheEvict(cacheNames = {
            CACHE_DISH_PAGES,
            CACHE_DISH_BY_CATEGORY,
            CACHE_DISH_ALL,
            CACHE_DISH_BY_ID
    }, allEntries = true)
    public void deleteDishById(long id) {
        archiveDishById(id);
    }

    /**
     * Restore an archived Dish so it appears on the menu again.
     */
    @CacheEvict(cacheNames = {
            CACHE_DISH_PAGES,
            CACHE_DISH_BY_CATEGORY,
            CACHE_DISH_ALL,
            CACHE_DISH_BY_ID
    }, allEntries = true)
    public void restoreDishById(long id) {
        dishRepository.findById(id).ifPresent(dish -> {
            dish.setArchived(false);
            dishRepository.save(dish);
        });
    }

    /**
     * Permanently delete a Dish and its files.
     */
    @CacheEvict(cacheNames = {
            CACHE_DISH_PAGES,
            CACHE_DISH_BY_CATEGORY,
            CACHE_DISH_ALL,
            CACHE_DISH_BY_ID
    }, allEntries = true)
    public void hardDeleteDish(long id) {
        dishRepository.deleteById(id);
        FolderDeleteUtil.deleteDishFolder(id);
    }
}
