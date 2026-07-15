package com.chronos.chronos.scheduler;

/** Canonical job lifecycle states (kept as constants for JSON-friendly String storage). */
public final class JobStatus {
    private JobStatus() {}

    public static final String SCHEDULED = "SCHEDULED";
    public static final String RUNNING = "RUNNING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String RETRYING = "RETRYING";
    public static final String CANCELLED = "CANCELLED";
}
