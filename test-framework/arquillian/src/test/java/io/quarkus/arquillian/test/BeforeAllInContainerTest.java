package io.quarkus.arquillian.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Verifies that {@code @BeforeAll} is invoked on the in container test instance, exactly once for the class. The counter
 * is static, so the value which the test method observes is the one of this class as loaded by the application class
 * loader, which is only incremented when the callback is invoked on the in container test instance.
 */
@ExtendWith(ArquillianExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BeforeAllInContainerTest {

    static final AtomicInteger BEFORE_ALL = new AtomicInteger();

    @Deployment
    public static JavaArchive createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClass(Foo.class);
    }

    @Inject
    Foo foo;

    @BeforeAll
    public static void beforeAll() {
        BEFORE_ALL.incrementAndGet();
    }

    @Test
    @Order(1)
    public void testBeforeAllWasInvokedInContainer() {
        assertNotNull(foo);
        assertEquals(1, BEFORE_ALL.get());
    }

    @Test
    @Order(2)
    public void testBeforeAllWasNotInvokedAgain() {
        assertEquals(1, BEFORE_ALL.get());
    }
}
