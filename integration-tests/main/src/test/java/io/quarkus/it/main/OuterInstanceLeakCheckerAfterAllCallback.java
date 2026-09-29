package io.quarkus.it.main;

import io.quarkus.test.junit.callback.QuarkusTestAfterAllCallback;
import io.quarkus.test.junit.callback.QuarkusTestContext;

/**
 * Asserts that the innermost class of {@link OuterInstanceLeakReproducerTestCase} reports exactly its two enclosing
 * instances, rather than an extra one created because the intermediate class was initialized a second time.
 */
public class OuterInstanceLeakCheckerAfterAllCallback implements QuarkusTestAfterAllCallback {

    @Override
    public void afterAll(QuarkusTestContext context) {
        if (!(context.getTestInstance() instanceof OuterInstanceLeakReproducerTestCase.Middle.Inner)) {
            return;
        }
        int actual = context.getOuterInstances().size();
        if (actual != 2) {
            throw new AssertionError("afterAll of the innermost nested class reported " + actual
                    + " outer instances, expected 2");
        }
    }
}
