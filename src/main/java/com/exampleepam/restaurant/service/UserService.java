package com.exampleepam.restaurant.service;

import com.exampleepam.restaurant.dto.UserCreationDto;
import com.exampleepam.restaurant.entity.User;
import com.exampleepam.restaurant.exception.EntityType;
import com.exampleepam.restaurant.exception.ExceptionManager;
import com.exampleepam.restaurant.exception.UserAlreadyExistAuthenticationException;
import com.exampleepam.restaurant.mapper.UserMapper;
import com.exampleepam.restaurant.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.exampleepam.restaurant.exception.ExceptionManager.getUserAlreadyExistsException;

/**
 * Service for the User entity
 */
@Service
public class UserService {
    UserRepository userRepository;
    UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    /**
     * Saves a User
     *
     * @param userCreationDto userCreationDTO to be mapped and saved
     * @throws UserAlreadyExistAuthenticationException if user already exists in DB
     *
     */
    public void register(UserCreationDto userCreationDto) throws UserAlreadyExistAuthenticationException {
        String email = userCreationDto.getEmail();
        var registredUser =  userRepository.findByEmail(email);
        if(registredUser != null) {
            throw getUserAlreadyExistsException(email);
        }

        userRepository.save(userMapper.toUser(userCreationDto));
    }

    /**
     * Returns the user's nalance
     *
     * @param id id of the user balance of which to be fetched
     *
     */
    public BigDecimal getUserBalance(long id) {
        return userRepository.getBalanceByUserId(id);
    }

    /**
     * Adds UAH to the user's balance
     *
     * @param id id of the user whose balance should be topped up
     * @param balanceToAdd amount of money to top up
     *
     */
    @Transactional
    public void addUserBalance(long id, BigDecimal balanceToAdd) {
        BigDecimal oldBalance = userRepository.getBalanceByUserId(id);
        BigDecimal newBalance = oldBalance.add(balanceToAdd);
        userRepository.setBalanceByUserId(id, newBalance);
    }


}
