package io.quarkus.test.junit.internal;

/**
 * Holds Quarkus test state across JUnit launcher sessions and Quarkus test classloaders in a Maven fork.
 */
public final class MavenForkState {

    private static Object state;
    private static String testingType;

    private MavenForkState() {
    }

    public static synchronized Object[] get() {
        return new Object[] { state, testingType };
    }

    public static synchronized void set(Object newState, String newTestingType) {
        state = newState;
        testingType = newTestingType;
    }
}
