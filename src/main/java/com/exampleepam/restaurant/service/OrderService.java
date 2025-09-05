package com.exampleepam.restaurant.service;

import com.exampleepam.restaurant.dto.order.OrderCreationDto;
import com.exampleepam.restaurant.dto.order.OrderResponseDto;
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
import com.exampleepam.restaurant.util.ServiceUtil;
import com.exampleepam.restaurant.service.DishForecastService;
import com.exampleepam.restaurant.service.IngredientForecastService;
import com.exampleepam.restaurant.service.forecast.ForecastModel;
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
import java.util.Optional;
import java.util.stream.Collectors;

import static com.exampleepam.restaurant.exception.ExceptionManager.*;

/**
 * Service for the Order entity
 */
@Service
public class OrderService {
    private static final long DEAFULT_USER_ID = 0L;
    private static final String STATUS_ALL = "all";
    private static final String STATUS_ACTIVE = "active";
    private static final List<Status> ACTIVE_STATUS_LIST = Arrays.asList(Status.PENDING,
            Status.COOKING, Status.DELIVERING);

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserRepository userRepository;
    private final DishRepository dishRepository;
    private final UserService userService;
    private final ServiceUtil serviceUtil;
    private final DishForecastService dishForecastService;
    private final IngredientForecastService ingredientForecastService;
    private final List<ForecastModel> models;

    public OrderService(OrderRepository orderRepository, OrderMapper orderMapper,
                        UserRepository userRepository, DishRepository dishRepository,
                        UserService userService, ServiceUtil serviceUtil,
                        DishForecastService dishForecastService,
                        IngredientForecastService ingredientForecastService,
                        List<ForecastModel> models) {

        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.userRepository = userRepository;
        this.dishRepository = dishRepository;
        this.userService = userService;
        this.serviceUtil = serviceUtil;
        this.dishForecastService = dishForecastService;
        this.ingredientForecastService = ingredientForecastService;
        this.models = models;
    }

    /**
     * Deletes an Order by id
     *
     * @param id the order's id
     */
    public void delete(long id) {
        orderRepository.deleteById(id);
    }

    /**
     * Returns a Paged object with a list of sorted dishes filtered by category
     *
     * @param currentPage current page
     * @param pageSize    number of rows per page
     * @param sortField   sort column for rows
     * @param sortDir     sort direction for rows
     * @param status      filter status, if status equals 'all' - it is ignored,
     *                    if 'active' - returns Orders with status in the Active status list,
     *                    else - filtered by the given status
     * @return a Paged object with a sorted and filtered by status list of OrderResponseDTOs
     */
    public Paged<OrderResponseDto> findPaginated(int currentPage, int pageSize, String sortField,
                                                 String sortDir, String status) {
        Sort sort = serviceUtil.getSort(sortField, sortDir);

        Pageable pageable = PageRequest.of(currentPage - 1, pageSize, sort);

        if (status.equals(STATUS_ALL)) {
            Page<Order> orderPage = orderRepository.findAll(pageable);
            Page<OrderResponseDto> orderResponseDtoPage = toDtoPage(orderPage);
            return new Paged<>(orderResponseDtoPage, Paging.of(orderPage.getTotalPages(), currentPage, pageSize));

        } else if (status.equals(STATUS_ACTIVE)) {
            Page<Order> orderPage = orderRepository.findOrdersWhereStatusOneOf(
                    ACTIVE_STATUS_LIST, pageable);
            Page<OrderResponseDto> orderResponseDtoPage = toDtoPage(orderPage);
            return new Paged<>(orderResponseDtoPage, Paging.of(orderPage.getTotalPages(), currentPage, pageSize));

        } else {
            Page<Order> orderPage = orderRepository
                    .findOrdersByStatus(Status.valueOf(status.toUpperCase(Locale.ENGLISH)), pageable);
            Page<OrderResponseDto> orderResponseDtoPage = toDtoPage(orderPage);
            return new Paged<>(orderResponseDtoPage, Paging.of(orderPage.getTotalPages(), currentPage, pageSize));
        }
    }

    /**
     * Returns a Paged object with a list of sorted dishes filtered by category and user
     *
     * @param currentPage       current page
     * @param pageSize          number of rows per page
     * @param sortField         sort column for rows
     * @param sortDir           sort direction for rows
     * @param status            filter status, if status equals 'all' - it is ignored,
     *                          if 'active' - returns Orders with status in the Active status list,
     *                          else - filtered by the given status
     * @param authenticatedUser filter orders by the given user
     * @return a Paged object with a sorted and filtered by status list of OrderResponseDTOs
     */
    public Paged<OrderResponseDto> findPaginatedByUser(int currentPage, int pageSize,
                                                       String sortField, String sortDir,
                                                       String status, AuthenticatedUser authenticatedUser) {
        Sort sort = serviceUtil.getSort(sortField, sortDir);

        Pageable pageable = PageRequest.of(currentPage - 1, pageSize, sort);
        long userId = userRepository.findByEmail(authenticatedUser.getUsername()).getId();

        if (status.equals(STATUS_ALL)) {
            Page<Order> orderPage = orderRepository.findAllOrdersByUserId(userId, pageable);
            Page<OrderResponseDto> orderResponseDtoPage = toDtoPage(orderPage);
            return new Paged<>(orderResponseDtoPage, Paging.of(orderPage.getTotalPages(), currentPage, pageSize));

        } else if (status.equals(STATUS_ACTIVE)) {
            Page<Order> orderPage = orderRepository.findOrdersByUserIdWhereStatusOneOf(
                    userId, ACTIVE_STATUS_LIST, pageable);
            Page<OrderResponseDto> orderResponseDtoPage = toDtoPage(orderPage);
            return new Paged<>(orderResponseDtoPage, Paging.of(orderPage.getTotalPages(), currentPage, pageSize));

        } else {
            Page<Order> orderPage = orderRepository
                    .findOrdersByStatusAndUserId(Status.valueOf(status.toUpperCase(Locale.ENGLISH)), userId, pageable);
            Page<OrderResponseDto> orderResponseDtoPage = toDtoPage(orderPage);
            return new Paged<>(orderResponseDtoPage, Paging.of(orderPage.getTotalPages(), currentPage, pageSize));
        }

    }


    private Page<OrderResponseDto> toDtoPage(Page<Order> orderPage) {
        return orderPage
                .map(orderMapper::toOrderResponseDto);
    }

    /**
     * Saves an Order
     *
     * @param orderCreationDto  OrderCreationDTO to be mapped to order and saved
     * @param authenticatedUser User to whom the order should be linked
     */
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

    /**
     * Change the order's status to 'DECLINED' and refund order's total cost the user
     *
     * @param id  Order's id
     * @throws javax.persistence.EntityNotFoundException if order does not exist
     */
    @Transactional
    public void setStatusDeclinedAndRefund(long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> getNotFoundException(EntityType.ORDER, id));
        order.setStatus(Status.DECLINED);

        BigDecimal orderCost = order.getTotalPrice();
        long userId = userRepository.getUserIdByOrderId(id);
        userService.addUserBalance(userId, orderCost);
    }

    public long getUserIdByOrderId(long id) {
        Optional<Order> order = orderRepository.findById(id);
        return order.map(value -> value.getUser().getId()).orElse(DEAFULT_USER_ID);
    }

    /**
     * Change the order's status to next by enum ordinal
     *
     * @param id  Order's id
     * @throws javax.persistence.EntityNotFoundException if order does not exist
     * @throws com.exampleepam.restaurant.exception.UnauthorizedActionException if it is an attempt to change
     * status of a 'DECLINED' or 'COMPLETED' order
     */
    @Transactional
    public void setNextStatus(long id) {
        Order order = orderRepository.findById(id).orElseThrow(() ->
                getNotFoundException(EntityType.ORDER, id));
        Status status = order.getStatus();
        if (status.equals(Status.DECLINED) || status.equals(Status.COMPLETED)) {
            throw getUnauthorizedActionException("Completed and Declined statuses cannot be changed");
        }
        Status nextStatus = Status.values()[status.ordinal() + 1];
        order.setStatus(nextStatus);
        order.setUpdateDateTime(LocalDateTime.now());
        if (nextStatus == Status.COMPLETED) {
            for (ForecastModel m : models) {
                dishForecastService.getDishForecasts(7, null, null, m.getName(), Pageable.unpaged());
                ingredientForecastService.getIngredientForecasts(7, null, null, m.getName(), Pageable.unpaged());
            }
        }
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
}
