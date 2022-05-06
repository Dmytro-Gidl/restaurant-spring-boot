package com.exampleepam.restaurant.entity.paging;

import lombok.*;

/**
 * PageItems describes a Page for Pagination.
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageItem {

    private PageItemType pageItemType;

    private int index;

    private boolean active;

}
