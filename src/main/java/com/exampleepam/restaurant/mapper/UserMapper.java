package com.exampleepam.restaurant.mapper;

import com.exampleepam.restaurant.dto.UserCreationDto;
import com.exampleepam.restaurant.entity.Role;
import com.exampleepam.restaurant.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class UserMapper {
    private PasswordEncoder passwordEncoder;

    public UserMapper(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }



    public User toUser(UserCreationDto userCreationDto) {
        return new User(userCreationDto.getName(),
                passwordEncoder.encode(userCreationDto.getPassword()),
                userCreationDto.getEmail(),
                Arrays.asList(Role.USER));
    }

}
