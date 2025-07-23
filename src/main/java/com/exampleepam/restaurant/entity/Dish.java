package com.exampleepam.restaurant.entity;

import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Describes Dish entity
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Dish extends AbstractBaseEntity{

    @Column(length = 30)
    private String name;
    @Column(length = 40)
    private String description;
    @Enumerated(EnumType.STRING)
    private Category category;
    BigDecimal price;

    /** Primary image filename */
    private String imageFileName;

    /** Additional images for the dish */
    @ElementCollection
    private List<String> galleryImageFileNames = new ArrayList<>();

    /**
     * Indicates whether this dish is archived (soft-deleted). Archived dishes
     * should not appear on the public menu but remain in the database together
     * with their reviews.
     */
    @Column(nullable = false)
    private boolean archived = false;

    @OneToMany(
            mappedBy = "dish",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @ToString.Exclude
    private List<Review> reviews = new ArrayList<>();

    public Dish(long id, String name, String description, Category category, BigDecimal price, String imagePath) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.imageFileName = imagePath;
    }

    /**
     * Returns list of all image filenames including the primary one.
     */
    public List<String> getAllImageFileNames() {
        List<String> result = new ArrayList<>();
        if (imageFileName != null) {
            result.add(imageFileName);
        }
        if (galleryImageFileNames != null) {
            for (String name : galleryImageFileNames) {
                if (!name.equals(imageFileName)) {
                    result.add(name);
                }
            }
        }
        return result;
    }

    @Transient
    public String getimagePath() {
        if (imageFileName == null || id == 0) return null;

        return "/dish-images/" + id + "/" + imageFileName;
    }

    @Transient
    public List<String> getImagePaths() {
        List<String> paths = new ArrayList<>();
        for (String name : getAllImageFileNames()) {
            paths.add("/dish-images/" + id + "/" + name);
        }
        return paths;
    }

    @Transient
    public String getImagePathsString() {
        return String.join(",", getImagePaths());
    }
}
