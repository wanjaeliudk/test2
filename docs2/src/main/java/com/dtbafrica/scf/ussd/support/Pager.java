package com.dtbafrica.scf.ussd.support;

import java.util.List;

/** Fixed-size paging for lists that will not fit one USSD screen. */
public final class Pager {

    private Pager() {}

    public record Page<T>(List<T> items, boolean hasNext) {}

    public static <T> Page<T> slice(List<T> all, int page, int size) {
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        return new Page<>(all.subList(from, to), to < all.size());
    }
}
