package com.exampleepam.restaurant.test_data;

import com.exampleepam.restaurant.dto.*;
import com.exampleepam.restaurant.entity.*;
import com.exampleepam.restaurant.entity.paging.Paged;
import com.exampleepam.restaurant.entity.paging.Paging;
import com.exampleepam.restaurant.security.AuthenticatedUser;
import org.aspectj.weaver.ast.Or;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class TestData {
public static final String USER_EMAIL = "userEmail@gmail.com";
public static final String ADMIN_EMAIL = "adminEmail@gmail.com";
    public static User getBasicUser() {
        return new User(1, "user", USER_EMAIL,
                "pas", BigDecimal.ZERO, Role.USER, new ArrayList<>(), true);

    }
// Basic Users
    public static User getAdminUser() {
        return new User(2, "admin", ADMIN_EMAIL,
                "pas", BigDecimal.ZERO, Role.ADMIN, new ArrayList<>(), true);

    }
    public static UserCreationDto getUserCreationDto() {
        return new UserCreationDto("user", "useremail@gmail.com",
                "pasW2ord", "pasW2ord");
    }
    // UserDetails
    public static AuthenticatedUser getAdminUserDetails() {
        return new AuthenticatedUser(getAdminUser());
    }
    public static AuthenticatedUser getBasicUserDetails() {
        return new AuthenticatedUser(getBasicUser());
    }
    // OrderCreationDTO
    public static OrderCreationDto getOrderCreationDTO() {
        String address = "Хрещатик, 1";
        Map<Long, Integer> dishQuantityMap = new HashMap<>();
        dishQuantityMap.put(1L, 2);
        dishQuantityMap.put(5L, 3);
        dishQuantityMap.put(4L, 1);
        dishQuantityMap.put(2L, 10);
        return new OrderCreationDto(address, dishQuantityMap);
    }
    // DishResposneDTO
    public static DishResponseDto getDishResponseDto() {
        return new DishResponseDto(2,"Dish1",
                "Description of Dish1", CategoryDto.DRINKS, BigDecimal.TEN, "image");
    }
    public static List<DishResponseDto> getDishResponseDtos() {
        DishResponseDto dishResponseDto1 = new DishResponseDto(2,"Dish1",
                "Description of Dish1", CategoryDto.DRINKS, BigDecimal.TEN, "image");
        DishResponseDto dishResponseDto2 = new DishResponseDto(3,"Dish2",
                "Description of Dish2", CategoryDto.SNACKS, BigDecimal.valueOf(25), "image2");
        DishResponseDto dishResponseDto3 = new DishResponseDto(3,"Dish3",
                "Description of Dish3", CategoryDto.SNACKS, BigDecimal.valueOf(45), "image3");
        return Arrays.asList(dishResponseDto1, dishResponseDto2, dishResponseDto3);
    }
    public static Paged<DishResponseDto> getDishResponseDtosPaged() {
        Pageable pageable = PageRequest.of(1, 10, Sort.by("id").ascending());

        Page<DishResponseDto> dishResponseDtoPage = new PageImpl<>(getDishResponseDtos(), pageable, 3);
        return new Paged<>(dishResponseDtoPage, Paging.of(1, 2, 3));
    }



    // OrderResponseDTO
    public static Paged<OrderResponseDto> getOrderResponseDtosPaged() {
        LocalDateTime dateTimeNow = LocalDateTime.now();


        OrderedItemResponseDto orderItem1 = new OrderedItemResponseDto("Dish1Name", 2);
        OrderedItemResponseDto orderItem2 = new OrderedItemResponseDto("Dish2Name", 5);
        OrderedItemResponseDto orderItem3 = new OrderedItemResponseDto("Dish3Name", 1);
        List<OrderedItemResponseDto> orderItems1 = Arrays.asList(orderItem1, orderItem2);
        List<OrderedItemResponseDto> orderItems2 = Arrays.asList(orderItem1, orderItem2, orderItem3);
        OrderResponseDto orderResponseDto1 = new OrderResponseDto(5, Status.DECLINED,
                "Address1", dateTimeNow, dateTimeNow, BigDecimal.valueOf(1155), orderItems1);
        OrderResponseDto orderResponseDto2 = new OrderResponseDto(5, Status.DECLINED,
                "Address1", dateTimeNow, dateTimeNow, BigDecimal.valueOf(1155), orderItems2);

        List<OrderResponseDto> responseDtos = Arrays.asList(orderResponseDto1, orderResponseDto2);
                Pageable pageable = PageRequest.of(1, 10, Sort.by("id").ascending());

        Page<OrderResponseDto> orderResponseDtoPage = new PageImpl<>(responseDtos, pageable, 3);
        return new Paged<>(orderResponseDtoPage, Paging.of(1, 2, 2));

    }
    // DishCreationDto
    public static DishCreationDto getDishCreationDto() {
        return new DishCreationDto(2, "image", "DishCreationName",
                "DishDescription", CategoryDto.DRINKS, BigDecimal.valueOf(213));
    }





}

