package io.quarkus.dev.testing;

import java.util.function.Consumer;

/**
 * Class that is used to notify test classes of failures on the server side.
 * <p>
 * This allows for server side exceptions to be added to the test failure
 */
public class ExceptionReporting {

    private static volatile Consumer<Throwable> listener;

    public static void notifyException(Throwable exception) {
        Consumer<Throwable> l = listener;
        if (l != null) {
            l.accept(exception);
        }
    }

    public static void setListener(Consumer<Throwable> l) {
        listener = l;
    }

    private ExceptionReporting() {
    }

}
