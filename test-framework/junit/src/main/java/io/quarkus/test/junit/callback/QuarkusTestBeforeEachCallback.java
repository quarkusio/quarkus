package io.quarkus.test.junit.callback;

/**
 * Can be implemented by classes that shall be called before each test method in a {@code @QuarkusTest}.
 * <p>
 * The implementing class has to be {@linkplain java.util.ServiceLoader deployed as service provider on the class path}.
 */
public interface QuarkusTestBeforeEachCallback {

    void beforeEach(QuarkusTestMethodContext context);
}
