package com.exampleepam.restaurant.forecast;

import com.exampleepam.restaurant.dto.dish.DishResponseDto;
import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.entity.Order;
import com.exampleepam.restaurant.entity.OrderItem;
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

        long userId = 1L;
        long dishId = 1L;
        Dish dish = new Dish();
        dish.setId(dishId);
        Review review = new Review();
        review.setDish(dish);
        Order order = new Order();
        OrderItem orderItem = new OrderItem(dish, 1);
        order.setOrderItems(List.of(orderItem));

        RatingData data = new RatingData(Map.of(userId, Map.of()), Map.of());
        when(reviewRepository.findAllWithUserAndDish()).thenReturn(List.of(review));
        when(orderRepository.findByStatus(Status.COMPLETED)).thenReturn(List.of(order));
        when(ratingMatrixBuilder.build(anyList(), anyList())).thenReturn(data);
        when(collaborativePredictor.predict(eq(userId), eq(data))).thenReturn(Map.of(dishId, 4.0));
        when(factorizationService.isReady()).thenReturn(true);
        when(dishRepository.findAllById(anySet())).thenReturn(List.of(dish));
        DishResponseDto dto = new DishResponseDto();
        dto.setId(dishId);
        when(dishMapper.toDishResponseDtoList(anyList())).thenReturn(List.of(dto));
        when(reviewRepository.getAverageRatingByDishId(dishId)).thenReturn(0.0);
        when(reviewRepository.countByDishId(dishId)).thenReturn(0L);

        List<DishResponseDto> result = service.getRecommendedDishes(userId, 1);
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

        long userId = 1L;
        long dishId = 10L;
        Dish dish = new Dish();
        dish.setId(dishId);
        Order order = new Order();
        OrderItem orderItem = new OrderItem(dish, 1);
        order.setOrderItems(List.of(orderItem));

        RatingData data = new RatingData(Map.of(userId, Map.of(dishId, 1.0)), Map.of(dishId, 1.0));
        when(reviewRepository.findAllWithUserAndDish()).thenReturn(List.of());
        when(orderRepository.findByStatus(Status.COMPLETED)).thenReturn(List.of(order));
        when(ratingMatrixBuilder.build(anyList(), anyList())).thenReturn(data);
        when(collaborativePredictor.predict(eq(userId), eq(data))).thenReturn(Map.of());
        when(factorizationService.isReady()).thenReturn(true);
        List<DishResponseDto> fallback = List.of(new DishResponseDto());
        when(categoryFallback.recommend(eq(userId), anySet(), eq(2))).thenReturn(fallback);

        List<DishResponseDto> result = service.getRecommendedDishes(userId, 2);
        assertEquals(fallback, result);
    }
}
