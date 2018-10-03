package org.jboss.protean.arc.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.util.TypeLiteral;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.ArcContainer;
import org.jboss.protean.arc.InstanceHandle;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class InjectionTest {

    @BeforeClass
    public static void init() {
        Arc.initialize();
    }

    @AfterClass
    public static void shutdown() {
        Arc.shutdown();
    }

    @Test
    public void testInjection() {
        ArcContainer arc = Arc.container();

        Baz baz = arc.instance(Baz.class).get();
        assertEquals("Lu Foo", baz.pingFoo());
        assertEquals(baz, arc.instance(Baz.class).get());

        InstanceHandle<Foo> foo = arc.instance(Foo.class, new MyQualifier.OneLiteral());
        assertEquals("Lu Foo", foo.get().ping());
        assertEquals("Lu Foo", foo.get().lazyPing());
        assertEquals(foo.get(), arc.instance(Foo.class, new MyQualifier.OneLiteral()).get());
        foo.destroy();
    }

    @Test
    public void testRequestContext() {
        ArcContainer arc = Arc.container();

        try {
            arc.instance(FooRequest.class).get().getId();
            fail();
        } catch (ContextNotActiveException expected) {
        }

        arc.requestContext().activate();

        FooRequest foo1 = arc.instance(FooRequest.class).get();
        assertEquals("Lu Foo", foo1.ping());
        FooRequest foo2 = arc.instance(FooRequest.class).get();
        assertEquals(foo1.getId(), foo2.getId());
        arc.requestContext().deactivate();

        try {
            arc.instance(FooRequest.class).get().getId();
            fail();
        } catch (ContextNotActiveException expected) {
        }
    }

    @Test
    public void testProducerMethodWithNormalScope() {
        // Bar#listOfNumbers
        InstanceHandle<List<Number>> list = Arc.container().instance(new TypeLiteral<List<Number>>() {
        });
        assertTrue(list.isAvailable());
        assertEquals(0, list.get().size());
    }

    @Test
    public void testInjectionPointMetadata() {
        ArcContainer arc = Arc.container();

        // Empty injection point
        assertEquals(Object.class.toString(), arc.instance(new TypeLiteral<List<String>>() {
        }, new MyQualifier.OneLiteral()).get().get(0));

        BazListProducerClient client = arc.instance(BazListProducerClient.class).get();
        List<String> list = client.getList();
        assertEquals("java.util.List<java.lang.String>", list.get(0));
    }
}
