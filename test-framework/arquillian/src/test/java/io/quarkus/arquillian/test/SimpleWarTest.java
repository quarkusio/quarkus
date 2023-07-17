package io.quarkus.arquillian.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArquillianExtension.class)
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
