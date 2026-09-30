package com.sleekydz86.catalog.global.application;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MigrationJobCancellationRegistry {

    private static final Map<String, AtomicBoolean> FLAGS = new ConcurrentHashMap<>();

    private MigrationJobCancellationRegistry() {
    }

    public static void register(String jobId) {
        FLAGS.put(jobId, new AtomicBoolean(false));
    }

    public static void requestCancel(String jobId) {
        FLAGS.computeIfAbsent(jobId, ignored -> new AtomicBoolean(false)).set(true);
    }

    public static boolean isCancelled(String jobId) {
        AtomicBoolean flag = FLAGS.get(jobId);
        return flag != null && flag.get();
    }

    public static void clear(String jobId) {
        FLAGS.remove(jobId);
    }
}
