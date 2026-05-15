package com.ecommerce.core.model;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {

    private long page;
    private long size;
    private long total;
    private long pages;
    private List<T> records;

    public static <T> PageResult<T> of(long page, long size, long total, List<T> records) {
        PageResult<T> r = new PageResult<>();
        r.page = page;
        r.size = size;
        r.total = total;
        r.pages = (total + size - 1) / size;
        r.records = records;
        return r;
    }
}
