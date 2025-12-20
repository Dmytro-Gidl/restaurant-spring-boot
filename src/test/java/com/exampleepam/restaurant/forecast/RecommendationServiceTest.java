package com.exampleepam.restaurant.forecast;

import com.exampleepam.restaurant.dto.dish.DishResponseDto;
import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.entity.Review;
import com.exampleepam.restaurant.entity.Status;
import com.exampleepam.restaurant.mapper.DishMapper;
import com.exampleepam.restaurant.repository.DishRepository;
import com.exampleepam.restaurant.repository.OrderRepository;
import com.exampleepam.restaurant.repository.ReviewRepository;
import com.exampleepam.restaurant.service.FactorizationService;
import com.exampleepam.restaurant.service.RecommendationService;
import com.exampleepam.restaurant.service.recommendation.RatingMatrixBuilder;
import com.exampleepam.restaurant.service.recommendation.RatingMatrixBuilder.RatingData;
import com.exampleepam.restaurant.service.recommendation.CollaborativePredictor;
import com.exampleepam.restaurant.service.recommendation.CategoryFallback;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class RecommendationServiceTest {

    @Test
    void returnsCollaborativePredictions() {
        DishRepository dishRepository = mock(DishRepository.class);
        DishMapper dishMapper = mock(DishMapper.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        FactorizationService factorizationService = mock(FactorizationService.class);
        RatingMatrixBuilder ratingMatrixBuilder = mock(RatingMatrixBuilder.class);
        CollaborativePredictor collaborativePredictor = mock(CollaborativePredictor.class);
        CategoryFallback categoryFallback = mock(CategoryFallback.class);

        RecommendationService service = new RecommendationService(dishRepository, dishMapper, reviewRepository, orderRepository,
                factorizationService, ratingMatrixBuilder, collaborativePredictor, categoryFallback);

        RatingData data = new RatingData(Map.of(2L, Map.of(1L, 1.0)), Map.of(2L, 1.0));
        Dish reviewedDish = new Dish();
        reviewedDish.setId(1L);
        Review review = new Review();
        review.setDish(reviewedDish);
        when(reviewRepository.findAllWithUserAndDish()).thenReturn(List.of(review));
        when(orderRepository.findByStatus(Status.COMPLETED)).thenReturn(List.of());
        when(ratingMatrixBuilder.build(anyList(), anyList())).thenReturn(data);
        when(collaborativePredictor.predict(eq(1L), eq(data))).thenReturn(Map.of(1L, 4.0));
        when(factorizationService.isReady()).thenReturn(true);
        Dish dish = new Dish();
        dish.setId(1L);
        when(dishRepository.findAllById(anySet())).thenReturn(List.of(dish));
        DishResponseDto dto = new DishResponseDto();
        dto.setId(1L);
        when(dishMapper.toDishResponseDtoList(anyList())).thenReturn(List.of(dto));
        when(reviewRepository.getAverageRatingByDishId(1L)).thenReturn(0.0);
        when(reviewRepository.countByDishId(1L)).thenReturn(0L);

        List<DishResponseDto> result = service.getRecommendedDishes(1L, 1);
        assertEquals(1, result.size());
        verify(categoryFallback, never()).recommend(anyLong(), anySet(), anyInt());
    }

    @Test
    void fallsBackWhenNoPredictions() {
        DishRepository dishRepository = mock(DishRepository.class);
        DishMapper dishMapper = mock(DishMapper.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        FactorizationService factorizationService = mock(FactorizationService.class);
        RatingMatrixBuilder ratingMatrixBuilder = mock(RatingMatrixBuilder.class);
        CollaborativePredictor collaborativePredictor = mock(CollaborativePredictor.class);
        CategoryFallback categoryFallback = mock(CategoryFallback.class);

        RecommendationService service = new RecommendationService(dishRepository, dishMapper, reviewRepository, orderRepository,
                factorizationService, ratingMatrixBuilder, collaborativePredictor, categoryFallback);

        RatingData data = new RatingData(Map.of(), Map.of());
        Dish reviewedDish = new Dish();
        reviewedDish.setId(1L);
        Review review = new Review();
        review.setDish(reviewedDish);
        when(reviewRepository.findAllWithUserAndDish()).thenReturn(List.of(review));
        when(orderRepository.findByStatus(Status.COMPLETED)).thenReturn(List.of());
        when(ratingMatrixBuilder.build(anyList(), anyList())).thenReturn(data);
        when(collaborativePredictor.predict(eq(1L), eq(data))).thenReturn(Map.of());
        List<DishResponseDto> fallback = List.of(new DishResponseDto());
        when(categoryFallback.recommend(eq(1L), anySet(), eq(2))).thenReturn(fallback);

        List<DishResponseDto> result = service.getRecommendedDishes(1L, 2);
        assertEquals(fallback, result);
    }
}
