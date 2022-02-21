package com.exampleepam.restaurant.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "users")
public class User extends AbstractBaseEntity{

    private String name;
    private String email;
    private String password;
    private BigDecimal balanceUAH;

    @Enumerated(EnumType.STRING)
    private Role role;
    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Order> orders;
    private boolean enabled;


    public User(long id, String name, String email, String password,
                BigDecimal balanceUAH, Role role, List<Order> orders, boolean enabled) {
        super(id);
        this.name = name;
        this.email = email;
        this.password = password;
        this.balanceUAH = balanceUAH;
        this.role = role;
        this.orders = orders;
        this.enabled = enabled;
    }

    public User(String name, String password, String email, Role role) {
        this.name = name;
        this.password = password;
        this.email = email;
        this.role = role;
        this.balanceUAH = BigDecimal.ZERO;
        this.enabled = true;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + name + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", roles=" + role +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User that = (User) o;
        return Objects.equals(this.email, that.email);
    }

    @Override
    public int hashCode() {
        if (id != 0) {
            return Objects.hash(id);
        } else {
            return super.hashCode();
        }
    }
}
