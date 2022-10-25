package io.quarkus.arquillian.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
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

    @Before
    public void before() {
        BEFORE.incrementAndGet();
    }

    @After
    public void after() {
        AFTER.incrementAndGet();
    }

    @Test
    @InSequence(1)
    public void testRunner() {
        assertNotNull(simple);
        assertNotNull(simple.config);
        assertEquals(1, BEFORE.get());
        assertEquals(0, AFTER.get());
    }

    @Test
    @InSequence(2)
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
