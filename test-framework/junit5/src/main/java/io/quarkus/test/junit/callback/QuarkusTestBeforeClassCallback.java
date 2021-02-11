package io.quarkus.test.junit.callback;

/**
 * Can be implemented by classes that shall be called once for each {@code @QuarkusTest} class before any test methods are
 * executed.
 * <p>
 * The implementing class has to be {@linkplain java.util.ServiceLoader deployed as service provider on the class path}.
 */
public interface QuarkusTestBeforeClassCallback {

    void beforeClass(Class<?> testClass);
}
