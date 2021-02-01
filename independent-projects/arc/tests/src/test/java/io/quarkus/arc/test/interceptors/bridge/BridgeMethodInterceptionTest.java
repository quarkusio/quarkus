package io.quarkus.arc.test.interceptors.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.interceptors.Counter;
import io.quarkus.arc.test.interceptors.Simple;
import io.quarkus.arc.test.interceptors.SimpleInterceptor;
import javax.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BridgeMethodInterceptionTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Base.class, Submarine.class, Ubot.class, Counter.class,
            Simple.class, SimpleInterceptor.class, ExampleApi.class, ExampleResource.class, AbstractResource.class);

    @Test
    public void testInterception() {
        ArcContainer container = Arc.container();
        Counter counter = container.instance(Counter.class).get();

        counter.reset();
        Submarine submarine = container.instance(Submarine.class).get();
        assertEquals("0foo1", submarine.echo("foo"));
        assertEquals(1, counter.get());

        // Now let's invoke the bridge method...
        Base<String> base = submarine;
        assertEquals("1foo2", base.echo("foo"));
        assertEquals(2, counter.get());

        counter.reset();
        Ubot ubot = container.instance(Ubot.class).get();
        assertEquals("011", ubot.echo(1));
        assertEquals(1, counter.get());

        // Now let's invoke the bridge method...
        Base<Integer> baseUbot = ubot;
        assertEquals("112", baseUbot.echo(1));
        assertEquals(2, counter.get());
    }

    @Disabled
    @Test
    public void testHierarchy() {
        ArcContainer container = Arc.container();
        Counter counter = container.instance(Counter.class).get();
        counter.reset();
        ExampleApi exampleApi = container.instance(ExampleApi.class).get();
        assertEquals("0foo1", exampleApi.create("foo"));
        assertEquals(1, counter.get());
    }

    static class Base<T> {

        String echo(T payload) {
            return payload.toString().toUpperCase();
        }

    }

    @ApplicationScoped
    static class Submarine extends Base<String> {

        @Simple
        @Override
        String echo(String payload) {
            return payload.toString();
        }

    }

    @Simple
    @ApplicationScoped
    static class Ubot extends Base<Integer> {

        @Override
        String echo(Integer payload) {
            return payload.toString();
        }

    }

}
