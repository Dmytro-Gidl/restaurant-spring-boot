package com.exampleepam.restaurant.entity;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "orders")
@ToString
public class Order extends AbstractBaseEntity{

    private String address;
    private LocalDateTime creationDateTime;
    private LocalDateTime updateDateTime;
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(
            mappedBy = "order",
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

    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }


    public void removeOrderItem(OrderItem orderItem) {
        orderItems.remove(orderItem);
        orderItem.setOrder(null);
    }


}

