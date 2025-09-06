package com.exampleepam.restaurant.service.recommendation;

import com.exampleepam.restaurant.dto.dish.DishResponseDto;
import com.exampleepam.restaurant.entity.Category;
import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.mapper.DishMapper;
import com.exampleepam.restaurant.repository.DishRepository;
import com.exampleepam.restaurant.repository.ReviewRepository;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class CategoryFallback {

    private final ReviewRepository reviewRepository;
    private final DishRepository dishRepository;
    private final DishMapper dishMapper;

    @Autowired
    public CategoryFallback(ReviewRepository reviewRepository,
                            DishRepository dishRepository,
                            DishMapper dishMapper) {
        this.reviewRepository = reviewRepository;
        this.dishRepository = dishRepository;
        this.dishMapper = dishMapper;
    }

    public List<DishResponseDto> recommend(long userId, Set<Long> excludeIds, int limit) {
        Set<Long> excluded = new HashSet<>(excludeIds);
        List<DishResponseDto> result = new ArrayList<>();
        List<Object[]> preferredCats = reviewRepository.findPreferredCategories(userId);
        for (Object[] row : preferredCats) {
            Category cat = (Category) row[0];
            List<Dish> dishes = dishRepository.findDishesByCategoryAndArchivedFalse(cat, Sort.by("name"));
            List<DishResponseDto> dtos = dishMapper.toDishResponseDtoList(dishes);
            assignAverageRatings(dtos);
            assignReviewCounts(dtos);
            dtos.sort(Comparator.comparing(DishResponseDto::getAverageRating).reversed());
            for (DishResponseDto dto : dtos) {
                if (excluded.contains(dto.getId())) continue;
                result.add(dto);
                excluded.add(dto.getId());
                if (result.size() >= limit) return result;
            }
        }
        return result;
    }

    private void assignAverageRatings(List<DishResponseDto> dtos) {
        for (DishResponseDto dto : dtos) {
            Double avg = reviewRepository.getAverageRatingByDishId(dto.getId());
            dto.setAverageRating(avg == null ? 0.0 : avg);
        }
    }

    private void assignReviewCounts(List<DishResponseDto> dtos) {
        for (DishResponseDto dto : dtos) {
            Long count = reviewRepository.countByDishId(dto.getId());
            dto.setReviewCount(count == null ? 0 : count);
        }
    }
}
