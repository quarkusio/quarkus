package io.quarkus.amazon.lambda.runtime;

public class TraceId {
    private static ThreadLocal<String> traceHeader = new ThreadLocal<>();

    public static void setTraceId(String id) {
        traceHeader.set(id);
    }

    public static String getTraceId() {
        return traceHeader.get();
    }

    public static void clearTraceId() {
        traceHeader.set(null);
    }
}
