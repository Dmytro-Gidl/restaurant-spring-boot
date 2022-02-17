package com.exampleepam.restaurant.service;

import com.exampleepam.restaurant.dto.OrderCreationDto;
import com.exampleepam.restaurant.dto.OrderResponseDto;
import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.entity.Order;
import com.exampleepam.restaurant.entity.Status;
import com.exampleepam.restaurant.entity.User;
import com.exampleepam.restaurant.entity.paging.Paged;
import com.exampleepam.restaurant.entity.paging.Paging;
import com.exampleepam.restaurant.exception.EntityType;
import com.exampleepam.restaurant.exception.ExceptionManager;
import com.exampleepam.restaurant.mapper.OrderMapper;
import com.exampleepam.restaurant.repository.DishRepository;
import com.exampleepam.restaurant.repository.OrderRepository;
import com.exampleepam.restaurant.repository.UserRepository;
import com.exampleepam.restaurant.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.exampleepam.restaurant.exception.ExceptionManager.*;

@Service
public class OrderService {
    OrderRepository orderRepository;
    OrderMapper orderMapper;
    UserRepository userRepository;
    DishRepository dishRepository;
    UserService userService;


    private static final String STATUS_ALL = "all";
    private static final String STATUS_ACTIVE = "active";
    private static final List<Status> ACTIVE_STATUS_LIST = Arrays.asList(Status.PENDING,
            Status.COOKING, Status.DELIVERING);

    public OrderService(OrderRepository orderRepository, OrderMapper orderMapper,
                        UserRepository userRepository, DishRepository dishRepository, UserService userService) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.userRepository = userRepository;
        this.dishRepository = dishRepository;
        this.userService = userService;
    }

    public List<Order> getOrders() {
        return orderRepository.findAll();
    }

    public void deleteOrderById(long id) {
        int delete = orderRepository.deleteOrderById(id);
        if (delete == 0) throw getNotFoundException(EntityType.ORDER, id);
    }


    public Paged<OrderResponseDto> findPaginated(int pageNo, int pageSize, String sortField,
                                                 String sortDir, String status) {
        Sort sort = getSort(sortField, sortDir);

        Pageable pageable = PageRequest.of(pageNo - 1, pageSize, sort);

        if (status.equals(STATUS_ALL)) {
            Page<Order> orderPage = orderRepository.findAll(pageable);
            Page<OrderResponseDto> orderResponseDtoPage = toDtoPage(orderPage);
            return new Paged<>(orderResponseDtoPage, Paging.of(orderPage.getTotalPages(), pageNo, pageSize));

        } else if (status.equals(STATUS_ACTIVE)) {
            Page<Order> orderPage = orderRepository.findOrdersWhereStatusOneOf(
                    ACTIVE_STATUS_LIST, pageable);
            Page<OrderResponseDto> orderResponseDtoPage = toDtoPage(orderPage);
            return new Paged<>(orderResponseDtoPage, Paging.of(orderPage.getTotalPages(), pageNo, pageSize));

        } else {
            Page<Order> orderPage = orderRepository
                    .findOrdersByStatus(Status.valueOf(status.toUpperCase(Locale.ENGLISH)), pageable);
            Page<OrderResponseDto> orderResponseDtoPage = toDtoPage(orderPage);
            return new Paged<>(orderResponseDtoPage, Paging.of(orderPage.getTotalPages(), pageNo, pageSize));
        }
    }

    private Sort getSort(String sortField, String sortDir) {
        Sort primarySort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Sort secondarySort = Sort.by("id").ascending();

        return primarySort.and(secondarySort);
    }

    public Paged<OrderResponseDto> findPaginatedByUser(int pageNo, int pageSize,
                                                       String sortField, String sortDir,
                                                       String status, AuthenticatedUser authenticatedUser) {
        Sort sort = getSort(sortField, sortDir);

        Pageable pageable = PageRequest.of(pageNo - 1, pageSize, sort);
        long userId = userRepository.findByEmail(authenticatedUser.getUsername()).getId();

        if (status.equals(STATUS_ALL)) {
            Page<Order> orderPage = orderRepository.findAllOrdersByUserId(userId, pageable);
            Page<OrderResponseDto> orderResponseDtoPage = toDtoPage(orderPage);
            return new Paged<>(orderResponseDtoPage, Paging.of(orderPage.getTotalPages(), pageNo, pageSize));

        } else if (status.equals(STATUS_ACTIVE)) {
            Page<Order> orderPage = orderRepository.findOrdersByUserIdWhereStatusOneOf(
                    userId, ACTIVE_STATUS_LIST, pageable);
            Page<OrderResponseDto> orderResponseDtoPage = toDtoPage(orderPage);
            return new Paged<>(orderResponseDtoPage, Paging.of(orderPage.getTotalPages(), pageNo, pageSize));

        } else {
            Page<Order> orderPage = orderRepository
                    .findOrdersByStatusAndUserId(Status.valueOf(status.toUpperCase(Locale.ENGLISH)), userId, pageable);
            Page<OrderResponseDto> orderResponseDtoPage = toDtoPage(orderPage);
            return new Paged<>(orderResponseDtoPage, Paging.of(orderPage.getTotalPages(), pageNo, pageSize));
        }

    }


    private Page<OrderResponseDto> toDtoPage(Page<Order> orderPage) {
        return orderPage
                .map(order -> orderMapper.toOrderResponseDto(order));
    }

    @Transactional
    public void saveOrder(OrderCreationDto orderCreationDto,
                          AuthenticatedUser authenticatedUser) {

        User user = userRepository.findByEmail(authenticatedUser.getUsername());
        Map<Long, Integer> dishIdQuantityMap = orderCreationDto.getDishIdQuantityMap();
        Map<Dish, Integer> dishQuantityMap = fetchDishesToMap(dishIdQuantityMap);

        Order order = orderMapper.toOrder(orderCreationDto, user, dishQuantityMap);
        throwExceptionIfMoneyInsufficient(user, order);
        withdrawOrderCostFromBalance(user, order);
        orderRepository.save(order);
    }

    private void throwExceptionIfMoneyInsufficient(User user, Order order) {
        BigDecimal userBalance = user.getBalanceUAH();
        BigDecimal orderCost = order.getTotalPrice();
        if (userBalance.compareTo(orderCost) < 0) {
            throw getInsufficientFundsException(user.getId(), userBalance, orderCost);
        }
    }

    private void withdrawOrderCostFromBalance(User user, Order order) {
        BigDecimal userBalance = user.getBalanceUAH();
        BigDecimal orderCost = order.getTotalPrice();
        BigDecimal newBalance = userBalance.subtract(orderCost);
        user.setBalanceUAH(newBalance);
    }

    private Map<Dish, Integer> fetchDishesToMap(Map<Long, Integer> dishIdQuantityMap) {
        return dishIdQuantityMap.entrySet().stream().collect(Collectors.toMap(
                e -> dishRepository.findById(e.getKey()).orElseThrow(()
                        -> ExceptionManager.getNotFoundException(EntityType.DISH, e.getKey())),
                Map.Entry::getValue));
    }

    @Transactional
    public void setStatusDeclinedAndRefund(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> getNotFoundException(EntityType.ORDER, orderId));
        order.setStatus(Status.DECLINED);

        BigDecimal orderCost = order.getTotalPrice();
        long userId = userRepository.getUserIdByOrderId(orderId);
        userService.addUserBalance(userId, orderCost);
    }

    @Transactional
    public void setNextStatus(long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() ->
                getNotFoundException(EntityType.ORDER, orderId));
        Status status = order.getStatus();
        if (status.equals(Status.DECLINED) || status.equals(Status.COMPLETED)) {
            throw getUnauthorizedActionException("Completed and Declined statuses cannot be changed");
        }
        Status nextStatus = Status.values()[status.ordinal() + 1];
        order.setStatus(nextStatus);
        order.setUpdateDateTime(LocalDateTime.now());
    }


}
