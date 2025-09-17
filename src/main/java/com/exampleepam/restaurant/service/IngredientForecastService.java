package com.exampleepam.restaurant.service;

import com.exampleepam.restaurant.entity.paging.Paged;
import com.exampleepam.restaurant.entity.paging.Paging;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Utility service that prepares forecast data for presentation.
 *
 * <p>The existing admin UI expects the service to always return a {@link Paged}
 * wrapper. When the calling code passes {@link Pageable#unpaged()} we used to
 * call {@link Pageable#getOffset()} directly which throws an
 * {@link UnsupportedOperationException}. The helper below makes sure that the
 * offset is only accessed when paging information is actually present.</p>
 */
@Service
public class IngredientForecastService {

    /**
     * Build a {@link Paged} response for the provided collection of forecast
     * items. The method is tolerant to {@link Pageable#unpaged()} and null
     * values and will simply return all elements in that case.
     *
     * @param forecasts raw forecast items to wrap in a {@link Page}
     * @param pageable  desired paging configuration, may be {@code null} or
     *                  {@link Pageable#unpaged()}
     * @param <T>       type of the items contained in the page
     * @return immutable {@link Paged} wrapper around the requested slice of the
     *         data
     */
    public <T> Paged<T> getIngredientForecasts(List<T> forecasts, Pageable pageable) {
        List<T> safeContent = forecasts == null ? List.of() : List.copyOf(forecasts);

        if (pageable == null || pageable.isUnpaged()) {
            Page<T> page = new PageImpl<>(safeContent);
            return new Paged<>(page, buildUnpagedPaging(page));
        }

        Page<T> page = buildPagedSlice(safeContent, pageable);
        Paging paging = Paging.of(page.getTotalPages(), pageable.getPageNumber() + 1, pageable.getPageSize());
        return new Paged<>(page, paging);
    }

    private <T> Page<T> buildPagedSlice(List<T> content, Pageable pageable) {
        int start = (int) Math.min(pageable.getOffset(), content.size());
        int end = Math.min(start + pageable.getPageSize(), content.size());
        List<T> slice = content.subList(start, end);
        return new PageImpl<>(slice, pageable, content.size());
    }

    private <T> Paging buildUnpagedPaging(Page<T> page) {
        if (!page.hasContent()) {
            Paging paging = new Paging();
            paging.setPageNumber(0);
            paging.setPageSize(0);
            paging.setTotalPages(0);
            paging.setNextEnabled(false);
            paging.setPrevEnabled(false);
            return paging;
        }

        return Paging.of(1, 1, page.getNumberOfElements());
    }
}
