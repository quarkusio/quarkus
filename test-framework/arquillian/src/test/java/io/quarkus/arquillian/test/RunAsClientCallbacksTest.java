package io.quarkus.arquillian.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Verifies that the callbacks of a test which runs as client, here because the deployment is declared as not testable,
 * are invoked exactly once. The test framework already invokes them on the test instance which it created itself, so
 * invoking them on the in container test instance as well would run them twice.
 * <p>
 * The counter is kept in a system property rather than in a static field, because the two test instances are loaded by
 * different class loaders and would therefore each have their own copy of a static field, which would hide the second
 * invocation from this test.
 */
@ExtendWith(ArquillianExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RunAsClientCallbacksTest {

    private static final String BEFORE_EACH_COUNT = RunAsClientCallbacksTest.class.getName() + ".beforeEachCount";

    @Deployment(testable = false)
    public static JavaArchive createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClass(Foo.class);
    }

    @BeforeEach
    public void beforeEach() {
        System.setProperty(BEFORE_EACH_COUNT, String.valueOf(count() + 1));
    }

    @Test
    @Order(1)
    public void testBeforeEachWasInvokedOnce() {
        assertEquals(1, count());
    }

    @Test
    @Order(2)
    public void testBeforeEachWasInvokedOnceMore() {
        assertEquals(2, count());
    }

    private static int count() {
        return Integer.parseInt(System.getProperty(BEFORE_EACH_COUNT, "0"));
    }
}
