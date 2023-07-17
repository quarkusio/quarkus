package io.quarkus.arc.test.interceptors.no.priority;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

/**
 * Tests that interceptor without @Priority will still be picked up and working with some default priority assigned.
 */
public class InterceptorWithNoPriorityTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Simple.class, AverageBean.class,
            SimpleInterceptorWithNoPriority.class);

    @Test
    public void testInterception() {
        assertFalse(SimpleInterceptorWithNoPriority.INTERCEPTOR_TRIGGERED);
        Arc.container().instance(AverageBean.class).get().ping();
        assertTrue(SimpleInterceptorWithNoPriority.INTERCEPTOR_TRIGGERED);
    }

}
