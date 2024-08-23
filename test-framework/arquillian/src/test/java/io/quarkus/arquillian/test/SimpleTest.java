package io.quarkus.arquillian.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArquillianExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SimpleTest {

    final static AtomicInteger BEFORE = new AtomicInteger();
    final static AtomicInteger AFTER = new AtomicInteger();

    @Deployment
    public static JavaArchive createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClass(SimpleClass.class);
    }

    @Inject
    SimpleClass simple;

    @BeforeEach
    public void before() {
        BEFORE.incrementAndGet();
    }

    @AfterEach
    public void after() {
        AFTER.incrementAndGet();
    }

    @Test
    @Order(1)
    public void testRunner() {
        assertNotNull(simple);
        assertNotNull(simple.config);
        assertEquals(1, BEFORE.get());
        assertEquals(0, AFTER.get());
    }

    @Test
    @Order(2)
    public void testAfter() {
        assertEquals(2, BEFORE.get());
        assertEquals(1, AFTER.get());
    }

    @Dependent
    public static class SimpleClass {

        @Inject
        Config config;

    }

}
