package io.quarkus.test.junit.callback;

/**
 * Implementations are called after JUnit constructs the test instance.
 * <p>
 * The implementing class has to be {@linkplain java.util.ServiceLoader deployed as service provider on the class path}.
 *
 * @deprecated Use {@link QuarkusTestAfterConstructCallback} instead
 */
@Deprecated
public interface QuarkusTestBeforeAllCallback {

    void beforeAll(Object testInstance);
}
