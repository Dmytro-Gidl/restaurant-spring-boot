package com.exampleepam.restaurant.entity;

import lombok.*;

import javax.persistence.*;
import java.util.Objects;

/**
 * Ingredient entity represents a raw product used in dishes.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ingredient extends AbstractBaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * Measurement unit used when specifying quantities of this ingredient.
     * Once set for an ingredient it is reused for all dishes referencing it.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeasureUnit unit;

    public Ingredient(long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ingredient that = (Ingredient) o;
        return Objects.equals(name.toLowerCase(), that.name.toLowerCase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase());
    }
}
