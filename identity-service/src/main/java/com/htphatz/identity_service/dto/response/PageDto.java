package com.htphatz.identity_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PageDto <T> {
    private List<T> items;
    private Integer page;
    private Integer size;
    private Integer totalPages;
    private Long totalItems;

    public <R> PageDto<R> map(Function<T, R> mapper) {
        final List<R> items = this.items.stream().map(mapper).toList();
        return new PageDto<>(items, page, size, totalPages, totalItems);
    }

    public static <R> PageDto<R> of(Page<R> page) {
        return new PageDto<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements());
    }
}
