package com.exampleepam.restaurant.dto.user;

import lombok.*;

/**
 * Response DTO for User
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserResponseDto {
    private long id;
    private String name;
    private String email;
}
