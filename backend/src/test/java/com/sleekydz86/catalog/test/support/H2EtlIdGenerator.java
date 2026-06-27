package com.sleekydz86.catalog.test.support;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class H2EtlIdGenerator {

    private static final Map<String, Integer> COUNTERS = new ConcurrentHashMap<>();

    private H2EtlIdGenerator() {
    }

    public static String nextId(String prefix) {
        String key = prefix.toUpperCase() + ":" + LocalDate.now();
        int next = COUNTERS.merge(key, 1, Integer::sum);
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return prefix.toLowerCase() + "-" + date + "-" + String.format("%03d", next);
    }
}
