package com.exampleepam.restaurant.mapper;

import com.exampleepam.restaurant.dto.user.UserCreationDto;
import com.exampleepam.restaurant.entity.Role;
import com.exampleepam.restaurant.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Mapper class for User and UserDTOs
 */
@Component
public class UserMapper {
    private final PasswordEncoder passwordEncoder;

    public UserMapper(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }


    public User toUser(UserCreationDto userCreationDto) {
        return new User(userCreationDto.getName(),
                passwordEncoder.encode(userCreationDto.getPassword()),
                userCreationDto.getEmail(),
                Role.ADMIN);
    }

}
