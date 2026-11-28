package io.quarkus.test.junit.internal;

/**
 * Holds Quarkus test state across JUnit launcher sessions and Quarkus test classloaders in a Maven fork.
 */
public final class MavenForkState {

    private static Object state;
    private static String testingType;
    private static Object valueRegistry;
    private static Object config;
    // Prevent repeated writes to a JUnit store, which is closed before some failure callbacks run.
    private static Object contextRoot;

    private MavenForkState() {
    }

    /**
     * @return state, testing type name, value registry, config and the last restored JUnit root context
     */
    public static synchronized Object[] get() {
        return new Object[] { state, testingType, valueRegistry, config, contextRoot };
    }

    public static synchronized void set(Object newState, String newTestingType, Object newValueRegistry, Object newConfig,
            Object newContextRoot) {
        state = newState;
        testingType = newTestingType;
        valueRegistry = newValueRegistry;
        config = newConfig;
        contextRoot = newContextRoot;
    }
}
