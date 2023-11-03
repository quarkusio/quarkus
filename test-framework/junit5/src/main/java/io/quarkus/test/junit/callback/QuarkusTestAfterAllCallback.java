package io.quarkus.test.junit.callback;

/**
 * Can be implemented by classes that shall be called after all test methods in a {@code @QuarkusTest} have been run.
 * <p>
 * The implementing class has to be {@linkplain java.util.ServiceLoader deployed as service provider on the class path}.
 */
public interface QuarkusTestAfterAllCallback {

    void afterAll(QuarkusTestContext context);
}
