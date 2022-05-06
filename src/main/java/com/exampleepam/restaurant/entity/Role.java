package com.exampleepam.restaurant.entity;

import org.springframework.security.core.GrantedAuthority;

/**
 * Describes Role enum for User entity
 */
public enum Role implements GrantedAuthority {
    ADMIN,
    USER;

    @Override
    public String getAuthority() {
        return this.name();
    }
}
