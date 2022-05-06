package com.exampleepam.restaurant.repository;

import com.exampleepam.restaurant.entity.Order;
import com.exampleepam.restaurant.entity.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findOrdersByStatus(Status status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.status IN :statusList")
    Page<Order> findOrdersWhereStatusOneOf(@Param(value = "statusList") List<Status> statusList, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.user.id = :id")
    Page<Order> findAllOrdersByUserId(@Param(value = "id") long id, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.status IN :statusList AND o.id = :id")
    Page<Order> findOrdersByUserIdWhereStatusOneOf(
            @Param(value = "id") long id,
            @Param(value = "statusList") List<Status> statusList, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.id = :id")
    Page<Order> findOrdersByStatusAndUserId(@Param(value = "status") Status status,
                                            @Param(value = "id") long id, Pageable pageable);
}
