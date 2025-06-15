package io.quarkus.test.junit.callback;

/**
 * Can be implemented by classes that shall be called immediately after a test method in a {@code @QuarkusTest}. These
 * callbacks run before {@link QuarkusTestAfterEachCallback} callbacks and are usually accompanied by
 * {@link QuarkusTestBeforeTestExecutionCallback}.
 * <p>
 * The implementing class has to be {@linkplain java.util.ServiceLoader deployed as service provider on the class path}.
 */
public interface QuarkusTestAfterTestExecutionCallback {

    void afterTestExecution(QuarkusTestMethodContext context);
}
