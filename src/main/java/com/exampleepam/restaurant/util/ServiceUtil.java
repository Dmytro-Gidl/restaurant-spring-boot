package com.exampleepam.restaurant.util;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Util class for Serivce
 */
@Component
public final class ServiceUtil {

    public Sort getSort(String sortField, String sortDir) {
        Sort primarySort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Sort secondarySort = Sort.by("id").ascending();

        return primarySort.and(secondarySort);
    }
}
