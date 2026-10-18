package io.quarkus.context.test.customContext;

/**
 * Custom context for test purposes.
 * Store some value (String) in a thread local variable.
 */
public class CustomContext {

    public static final String NAME = "MyContext";
    private static ThreadLocal<String> context = ThreadLocal.withInitial(() -> "");

    private CustomContext() {
    }

    public static String get() {
        return context.get();
    }

    public static void set(String label) {
        context.set(label);
    }
}
