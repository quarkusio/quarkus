package io.quarkus.arquillian.test;

import static org.junit.Assert.assertNotNull;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class SimpleTest {

    @Deployment
    public static JavaArchive createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClass(SimpleClass.class);
    }

    @Inject
    SimpleClass simple;

    @Test
    public void testRunner() {
        assertNotNull(simple);
        assertNotNull(simple.config);
    }

    @Dependent
    public static class SimpleClass {

        @Inject
        Config config;

    }

}
