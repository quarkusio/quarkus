package io.quarkus.resteasy.reactive.server.test;

public final class ExceptionUtil {

    private static final StackTraceElement[] EMPTY_STACK_TRACE = new StackTraceElement[0];

    private ExceptionUtil() {
    }

    public static <T extends Throwable> T removeStackTrace(T t) {
        t.setStackTrace(EMPTY_STACK_TRACE);
        return t;
    }
}
