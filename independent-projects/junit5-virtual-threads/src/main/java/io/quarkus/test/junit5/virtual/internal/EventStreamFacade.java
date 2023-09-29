package io.quarkus.test.junit5.virtual.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Consumer;

import org.junit.jupiter.api.function.ThrowingSupplier;

import jdk.jfr.EventSettings;
import jdk.jfr.consumer.RecordedEvent;

/**
 * The RecordingStream is only Java 14+, and the code must be Java 11.
 * This class provides the used API, but under the hood use MethodHandle.
 */
public class EventStreamFacade {
    public static final String CARRIER_PINNED_EVENT_NAME = "jdk.VirtualThreadPinned";

    /**
     * Whether the RecordingStream API is available.
     */
    public static final boolean available;

    private static final MethodHandle constructor;

    private static final MethodHandle enableMethod;

    private static final MethodHandle stopMethod;

    private static final MethodHandle startAsyncMethod;

    private static final MethodHandle setMaxSizeMethod;

    private static final MethodHandle setOrderedMethod;

    private static final MethodHandle onEventMethod;

    static {
        boolean en;
        MethodHandle tempConstructor = null;
        MethodHandle tempEnable = null;
        MethodHandle tempStartAsync = null;
        MethodHandle tempStop = null;
        MethodHandle tempSetMaxSize = null;
        MethodHandle tempSetOrdered = null;
        MethodHandle tempOnEvent = null;
        try {
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            var clazz = EventStreamFacade.class.getClassLoader().loadClass("jdk.jfr.consumer.RecordingStream");
            tempConstructor = lookup.findConstructor(clazz, MethodType.methodType(void.class));
            tempEnable = lookup.findVirtual(clazz, "enable", MethodType.methodType(EventSettings.class, String.class));
            tempSetMaxSize = lookup.findVirtual(clazz, "setMaxSize", MethodType.methodType(void.class, long.class));
            tempSetOrdered = lookup.findVirtual(clazz, "setOrdered", MethodType.methodType(void.class, boolean.class));
            tempOnEvent = lookup.findVirtual(clazz, "onEvent", MethodType.methodType(void.class, Consumer.class));
            tempStartAsync = lookup.findVirtual(clazz, "startAsync", MethodType.methodType(void.class));
            tempStop = lookup.findVirtual(clazz, "stop", MethodType.methodType(boolean.class));
            en = true;
        } catch (Throwable e) {
            en = false;
        }
        available = en;
        constructor = tempConstructor;
        enableMethod = tempEnable;
        startAsyncMethod = tempStartAsync;
        stopMethod = tempStop;
        setMaxSizeMethod = tempSetMaxSize;
        setOrderedMethod = tempSetOrdered;
        onEventMethod = tempOnEvent;
    }

    private final Object stream;

    public EventStreamFacade() {
        try {
            this.stream = constructor.invoke();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T invoke(ThrowingSupplier<T> invocation) {
        if (!available) {
            throw new UnsupportedOperationException("Stream recording not configured correctly, make sure you use Java 14+");
        }
        try {
            return invocation.get();
        } catch (Throwable e) {
            throw new RuntimeException("Unable to invoke event stream method", e);
        }
    }

    public EventSettings enable(String event) {
        return invoke(() -> (EventSettings) enableMethod.invoke(stream, event));
    }

    public void startAsync() {
        invoke(() -> startAsyncMethod.invoke(stream));
    }

    public void setMaxSize(int max) {
        invoke(() -> setMaxSizeMethod.invoke(stream, max));
    }

    public void setOrdered(boolean ordered) {
        invoke(() -> setOrderedMethod.invoke(stream, ordered));
    }

    public void onEvent(Consumer<RecordedEvent> consumer) {
        invoke(() -> onEventMethod.invoke(stream, consumer));
    }

    public boolean stop() {
        return invoke(() -> (boolean) stopMethod.invoke(stream));
    }

}
