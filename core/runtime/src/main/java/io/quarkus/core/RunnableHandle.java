package io.quarkus.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

/**
 * A simple wrapper around a method handle and captured value which allows
 * invocation of the handle via the {@code Runnable} interface
 * without involving the lambda metafactory.
 */
final class RunnableHandle implements Runnable {
    private final MethodHandle handle;
    private final Object argument;

    /**
     * Construct a new instance.
     *
     * @param handle the method handle which accepts a single argument (must not be {@code null})
     * @param argument the argument to pass to the handle
     */
    public RunnableHandle(final MethodHandle handle, final Object argument) {
        this.handle = handle.asType(MethodType.methodType(void.class, Object.class));
        this.argument = argument;
    }

    public void run() {
        try {
            handle.invokeExact(argument);
        } catch (Throwable e) {
            throw sneak(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneak(Throwable e) throws E {
        throw (E) e;
    }
}
