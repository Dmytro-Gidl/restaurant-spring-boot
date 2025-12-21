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
import com.exampleepam.restaurant.service.recommendation.CategoryFallback;
import com.exampleepam.restaurant.service.recommendation.CollaborativePredictor;
import com.exampleepam.restaurant.service.recommendation.RatingMatrixBuilder;
import com.exampleepam.restaurant.service.recommendation.RatingMatrixBuilder.RatingData;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService")
class RecommendationServiceTest {

    private static final long USER_ID = 1L;

    private static final long DISH_ID_RATED = 10L;
    private static final long DISH_ID_CANDIDATE = 20L;
    private static final long DISH_ID_PRIMARY = 30L;
    private static final long DISH_ID_FALLBACK = 99L;

    private static final int LIMIT_ONE = 1;
    private static final int LIMIT_TWO = 2;

    private DishRepository dishRepository;
    private DishMapper dishMapper;
    private ReviewRepository reviewRepository;
    private OrderRepository orderRepository;
    private FactorizationService factorizationService;
    private RatingMatrixBuilder ratingMatrixBuilder;
    private CollaborativePredictor collaborativePredictor;
    private CategoryFallback categoryFallback;

    private RecommendationService service;

    @BeforeEach
    void setUp() {
        dishRepository = mock(DishRepository.class);
        dishMapper = mock(DishMapper.class);
        reviewRepository = mock(ReviewRepository.class);
        orderRepository = mock(OrderRepository.class);
        factorizationService = mock(FactorizationService.class);
        ratingMatrixBuilder = mock(RatingMatrixBuilder.class);
        collaborativePredictor = mock(CollaborativePredictor.class);
        categoryFallback = mock(CategoryFallback.class);

        service = new RecommendationService(
                dishRepository,
                dishMapper,
                reviewRepository,
                orderRepository,
                factorizationService,
                ratingMatrixBuilder,
                collaborativePredictor,
                categoryFallback
        );
    }

    // ------------------ tests ------------------

    @Test
    @DisplayName("returns empty list when limit is non-positive")
    void returnsEmpty_whenLimitNonPositive() {
        assertEquals(List.of(), service.getRecommendedDishes(USER_ID, 0));
        assertEquals(List.of(), service.getRecommendedDishes(USER_ID, -1));

        verifyNoInteractions(reviewRepository, orderRepository, ratingMatrixBuilder,
                collaborativePredictor, factorizationService, dishRepository, dishMapper, categoryFallback);
    }

    @Test
    @DisplayName("returns empty list when there is no history (no reviews and no completed orders)")
    void returnsEmpty_whenNoReviewsAndNoOrders() {
        when(reviewRepository.findAllWithUserAndDish()).thenReturn(List.of());
        when(orderRepository.findByStatus(Status.COMPLETED)).thenReturn(List.of());

        List<DishResponseDto> result = service.getRecommendedDishes(USER_ID, LIMIT_ONE);

        assertTrue(result.isEmpty());
        verifyNoInteractions(ratingMatrixBuilder, collaborativePredictor, factorizationService, dishRepository, dishMapper, categoryFallback);
    }

    @Test
    @DisplayName("does NOT fallback when CF has no predictions but MF can score candidates")
    void doesNotFallback_whenCfEmptyButMfHasCandidates() {
        // reviews contain a dish -> collectAllDishIds() gives candidates :contentReference[oaicite:1]{index=1}
        when(reviewRepository.findAllWithUserAndDish()).thenReturn(List.of(reviewWithDishId(DISH_ID_CANDIDATE)));
        when(orderRepository.findByStatus(Status.COMPLETED)).thenReturn(List.of());

        // user has not rated anything
        when(ratingMatrixBuilder.build(anyList(), anyList()))
                .thenReturn(ratingData(matrixForUser(USER_ID, Map.of())));

        // ready -> no training branch :contentReference[oaicite:2]{index=2}
        when(factorizationService.isReady()).thenReturn(true);

        // dish fetch + mapping path :contentReference[oaicite:3]{index=3}
        stubDishFetchAndMapperIdentity();

        List<DishResponseDto> result = service.getRecommendedDishes(USER_ID, LIMIT_ONE);

        assertEquals(1, result.size());
        assertEquals(DISH_ID_CANDIDATE, result.get(0).getId());

        verify(categoryFallback, never()).recommend(anyLong(), anySet(), anyInt());
    }

    @Test
    @DisplayName("uses fallback only when blended becomes empty after filtering already-known dishes")
    void usesFallbackOnly_whenAllSignalsFilteredOut() {
        // Reviews mention only the rated dish
        when(reviewRepository.findAllWithUserAndDish()).thenReturn(List.of(reviewWithDishId(DISH_ID_RATED)));
        when(orderRepository.findByStatus(Status.COMPLETED)).thenReturn(List.of());

        // user already rated DISH_ID_RATED -> candidateIds becomes empty after removeAll :contentReference[oaicite:4]{index=4}
        when(ratingMatrixBuilder.build(anyList(), anyList()))
                .thenReturn(ratingData(matrixForUser(USER_ID, Map.of(DISH_ID_RATED, 5.0))));

        // CF predicts only the already-rated dish
        when(collaborativePredictor.predict(eq(USER_ID), any(RatingData.class)))
                .thenReturn(Map.of(DISH_ID_RATED, 4.5));

        when(factorizationService.isReady()).thenReturn(true);

        List<DishResponseDto> fallback = List.of(dto(DISH_ID_FALLBACK));
        when(categoryFallback.recommend(eq(USER_ID), eq(Set.of(DISH_ID_RATED)), eq(LIMIT_ONE)))
                .thenReturn(fallback);

        List<DishResponseDto> result = service.getRecommendedDishes(USER_ID, LIMIT_ONE);

        assertEquals(fallback, result);

        // When blended is empty -> fallback only and return immediately :contentReference[oaicite:5]{index=5}
        verifyNoInteractions(dishRepository, dishMapper);
    }

    @Test
    @DisplayName("never returns already-rated dishes even if CF predicts them")
    void excludesAlreadyRatedDish_evenIfCfPredictsIt() {
        // reviews include rated dish and another dish
        when(reviewRepository.findAllWithUserAndDish()).thenReturn(List.of(
                reviewWithDishId(DISH_ID_RATED),
                reviewWithDishId(DISH_ID_CANDIDATE)
        ));
        when(orderRepository.findByStatus(Status.COMPLETED)).thenReturn(List.of());

        when(ratingMatrixBuilder.build(anyList(), anyList()))
                .thenReturn(ratingData(matrixForUser(USER_ID, Map.of(DISH_ID_RATED, 5.0))));

        // CF tries to recommend rated + candidate
        when(collaborativePredictor.predict(eq(USER_ID), any(RatingData.class)))
                .thenReturn(Map.of(DISH_ID_RATED, 10.0, DISH_ID_CANDIDATE, 1.0));

        when(factorizationService.isReady()).thenReturn(true);

        stubDishFetchAndMapperIdentity();

        List<DishResponseDto> result = service.getRecommendedDishes(USER_ID, LIMIT_ONE);

        assertEquals(1, result.size());
        assertEquals(DISH_ID_CANDIDATE, result.get(0).getId());
        assertNotEquals(DISH_ID_RATED, result.get(0).getId());
        verify(categoryFallback, never()).recommend(anyLong(), anySet(), anyInt());
    }

    @Test
    @DisplayName("merges fallback when primary recommendations are fewer than limit")
    void mergesFallback_whenNotEnoughPrimaryResults() {
        when(reviewRepository.findAllWithUserAndDish()).thenReturn(List.of(reviewWithDishId(DISH_ID_PRIMARY)));
        when(orderRepository.findByStatus(Status.COMPLETED)).thenReturn(List.of());

        when(ratingMatrixBuilder.build(anyList(), anyList()))
                .thenReturn(ratingData(matrixForUser(USER_ID, Map.of())));

        when(factorizationService.isReady()).thenReturn(true);

        stubDishFetchAndMapperIdentity();

        List<DishResponseDto> fallback = List.of(dto(DISH_ID_FALLBACK));
        when(categoryFallback.recommend(eq(USER_ID), anySet(), eq(1)))
                .thenReturn(fallback);

        List<DishResponseDto> result = service.getRecommendedDishes(USER_ID, LIMIT_TWO);

        assertEquals(LIMIT_TWO, result.size());
        assertEquals(DISH_ID_PRIMARY, result.get(0).getId());
        assertEquals(DISH_ID_FALLBACK, result.get(1).getId());

        // usedIds must contain the already-selected primary dish :contentReference[oaicite:6]{index=6}
        ArgumentCaptor<Set<Long>> captor = ArgumentCaptor.forClass(Set.class);
        verify(categoryFallback).recommend(eq(USER_ID), captor.capture(), eq(1));
        assertTrue(captor.getValue().contains(DISH_ID_PRIMARY));
    }

    @Test
    @DisplayName("trains factorization model when not ready")
    void trainsFactorization_whenNotReady() {
        when(reviewRepository.findAllWithUserAndDish()).thenReturn(List.of(reviewWithDishId(DISH_ID_PRIMARY)));
        when(orderRepository.findByStatus(Status.COMPLETED)).thenReturn(List.of());

        when(ratingMatrixBuilder.build(anyList(), anyList()))
                .thenReturn(ratingData(matrixForUser(USER_ID, Map.of())));

        when(factorizationService.isReady()).thenReturn(false);
        when(factorizationService.rmseOnReviews(anyList())).thenReturn(0.123);

        stubDishFetchAndMapperIdentity();

        List<DishResponseDto> result = service.getRecommendedDishes(USER_ID, LIMIT_ONE);

        assertEquals(1, result.size());
        verify(factorizationService).train(anyList(), anyList());
        verify(factorizationService).rmseOnReviews(anyList());
    }

    // ------------------ helpers ------------------

    private void stubDishFetchAndMapperIdentity() {
        when(dishRepository.findAllById(anyList())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            List<Long> ids = (List<Long>) inv.getArgument(0);
            if (ids == null) return List.of();
            return ids.stream().map(this::dish).collect(Collectors.toList());
        });

        when(dishMapper.toDishResponseDtoList(anyList())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            List<Dish> dishes = (List<Dish>) inv.getArgument(0);
            if (dishes == null) return List.of();
            List<DishResponseDto> out = new ArrayList<>();
            for (Dish d : dishes) {
                DishResponseDto dto = new DishResponseDto();
                dto.setId(d.getId());
                out.add(dto);
            }
            return out;
        });
    }

    private static RatingData ratingData(Map<Long, Map<Long, Double>> matrix) {
        // second argument is ignored by RecommendationService (it uses only matrix()) :contentReference[oaicite:7]{index=7}
        return new RatingData(matrix, Map.of());
    }

    private static Map<Long, Map<Long, Double>> matrixForUser(long userId, Map<Long, Double> ratings) {
        return Map.of(userId, ratings);
    }

    private Dish dish(long id) {
        Dish d = new Dish();
        d.setId(id);
        return d;
    }

    private static DishResponseDto dto(long id) {
        DishResponseDto d = new DishResponseDto();
        d.setId(id);
        return d;
    }

    private static Review reviewWithDishId(long dishId) {
        Dish dish = new Dish();
        dish.setId(dishId);
        Review r = new Review();
        r.setDish(dish);
        return r;
    }
}
