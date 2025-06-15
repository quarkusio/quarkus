package io.quarkus.test.junit.callback;

/**
 * Can be implemented by classes that shall be called right before a test method in a {@code @QuarkusTest}. These
 * callbacks run after {@link QuarkusTestBeforeEachCallback} callbacks and are usually accompanied by
 * {@link QuarkusTestAfterTestExecutionCallback}.
 * <p>
 * The implementing class has to be {@linkplain java.util.ServiceLoader deployed as service provider on the class path}.
 */
public interface QuarkusTestBeforeTestExecutionCallback {

    void beforeTestExecution(QuarkusTestMethodContext context);
}
