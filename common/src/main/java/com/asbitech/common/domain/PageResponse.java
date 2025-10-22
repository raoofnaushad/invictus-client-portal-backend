package com.asbitech.common.domain;

import java.util.List;

import lombok.Getter;


@Getter
public class PageResponse<T> {
    private final List<T> content;
    private final int pageNumber;
    private final int pageSize;
    private final long totalElements;
    private final int totalPages;

    public PageResponse(List<T> content, int pageNumber, int pageSize, long totalElements) {
        this.content = List.copyOf(content);
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = (int) Math.ceil((double) totalElements / pageSize);
    }
}
