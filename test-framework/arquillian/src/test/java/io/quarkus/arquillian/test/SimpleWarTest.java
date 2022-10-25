package io.quarkus.arquillian.test;

import static org.junit.Assert.assertNotNull;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class SimpleWarTest {

    @Deployment
    public static WebArchive createTestArchive() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsLibrary(ShrinkWrap.create(JavaArchive.class).addClass(Foo.class)
                        .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"))
                .addClass(SimpleClass.class);
    }

    @Inject
    SimpleClass simple;

    @Test
    public void testRunner() {
        assertNotNull(simple);
        assertNotNull(simple.config);
        assertNotNull(simple.foo);
    }

}
