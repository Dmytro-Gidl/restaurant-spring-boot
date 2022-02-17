package com.exampleepam.restaurant.repository;

import com.exampleepam.restaurant.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);


    @Query("SELECT u.balanceUAH FROM User u WHERE u.id=:id")
    BigDecimal getBalanceByUserId(@Param(value = "id") long id);

    @Modifying
    @Query("UPDATE User u SET u.balanceUAH=:balanceUAH  WHERE u.id=:id")
    void setBalanceByUserId(@Param(value = "id") long id,
                            @Param(value = "balanceUAH") BigDecimal balanceUAH);

    @Query("SELECT u.id FROM User u JOIN u.orders o WHERE o.id =:id")
    long getUserIdByOrderId(@Param(value = "id") long orderId);
}
