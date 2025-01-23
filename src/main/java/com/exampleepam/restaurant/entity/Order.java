package com.exampleepam.restaurant.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Describes Order entity
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "orders")
@ToString
public class Order extends AbstractBaseEntity {

    @Column(length = 35)
    private String address;
    private LocalDateTime creationDateTime;
    private LocalDateTime updateDateTime;
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Review review;

    @OneToMany(
            mappedBy = "order",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @ToString.Exclude
    private List<OrderItem> orderItems;

    public Order(long id, Status status, BigDecimal totalPrice,
                 String address, User user) {
        this.id = id;
        this.status = status;
        this.creationDateTime = LocalDateTime.now();
        this.updateDateTime = LocalDateTime.now();
        this.totalPrice = totalPrice;
        this.address = address;
        this.user = user;
        this.orderItems = new ArrayList<>();
    }

    public Order(Long id, String address, LocalDateTime creationDateTime, LocalDateTime updateDateTime,
                 BigDecimal totalPrice, Status status, User user, Review review,
                 List<OrderItem> orderItems) {
        this.id = id;
        this.address = address;
        this.creationDateTime = creationDateTime;
        this.updateDateTime = updateDateTime;
        this.totalPrice = totalPrice;
        this.status = status;
        this.user = user;
        this.review = review;
        this.orderItems = orderItems;
    }

    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }


    public void removeOrderItem(OrderItem orderItem) {
        orderItems.remove(orderItem);
        orderItem.setOrder(null);
    }


}

