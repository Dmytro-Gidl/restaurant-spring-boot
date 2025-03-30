package com.exampleepam.restaurant.dto.user;

import com.exampleepam.restaurant.validator.PasswordMatches;
import com.exampleepam.restaurant.validator.ValidEmail;
import com.exampleepam.restaurant.validator.ValidPassword;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

/**
 * Creation DTO for User
 */
@Getter
@Setter
@NoArgsConstructor
@PasswordMatches(message = "{fail.matches.passwords}")
public class UserCreationDto {
    @NotBlank(message = "{fail.blank.name}")
    @Length(min = 2, max = 35, message ="{fail.size.name}")
    private String name;

    @NotBlank(message = "{fail.blank.email}")
    @ValidEmail(message = "{fail.invalid.email}")
    private String email;

    @NotBlank(message = "{fail.blank.password}")
    @ValidPassword
    private String password;
    private String matchingPassword;


    public UserCreationDto(String name, String email, String password, String matchingPassword) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.matchingPassword = matchingPassword;
    }

    @Override
    public String toString() {
        return "UserDto{" +
                "username='" + name + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", matchingPassword='" + matchingPassword + '\'' +
                '}';
    }
}

