package io.quarkus.dev.testing;

public class TracingHandler {

    private static volatile TraceListener tracingHandler;

    public static void trace(String className) {
        TraceListener t = tracingHandler;
        if (t != null) {
            t.touched(className);
        }
    }

    public static void quarkusStarting() {
        TraceListener t = tracingHandler;
        if (t != null) {
            t.quarkusStarting();
        }
    }

    public static void quarkusStopping() {
        TraceListener t = tracingHandler;
        if (t != null) {
            t.quarkusStopping();
        }
    }

    public static void quarkusStarted() {
        TraceListener t = tracingHandler;
        if (t != null) {
            t.quarkusStarted();
        }
    }

    public static void quarkusStopped() {
        TraceListener t = tracingHandler;
        if (t != null) {
            t.quarkusStopped();
        }
    }

    public static void setTracingHandler(TraceListener tracingHandler) {
        TracingHandler.tracingHandler = tracingHandler;
    }

    public interface TraceListener {

        void touched(String className);

        default void quarkusStarting() {
        }

        default void quarkusStopping() {
        }

        default void quarkusStarted() {
        }

        default void quarkusStopped() {
        }
    }
}
