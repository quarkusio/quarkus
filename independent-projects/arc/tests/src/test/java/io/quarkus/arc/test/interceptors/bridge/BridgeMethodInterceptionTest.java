package io.quarkus.arc.test.interceptors.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.interceptors.Counter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BridgeMethodInterceptionTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Base.class, Submarine.class, Ubot.class, Ponorka.class,
            Counter.class, Simple.class, SimpleInterceptor.class, ExampleApi.class, ExampleResource.class,
            AbstractResource.class, NextBase.class, NextSubmarine.class);

    @Test
    public void testInterception() {
        ArcContainer container = Arc.container();
        Counter counter = container.instance(Counter.class).get();

        counter.reset();
        Submarine submarine = container.instance(Submarine.class).get();
        assertEquals("foo", submarine.echo("foo"));
        assertEquals(Submarine.class.getSimpleName(), submarine.getName());
        assertEquals(2, counter.get());
        // Now let's invoke the bridge method...
        Base<String> base = submarine;
        assertEquals("foo", base.echo("foo"));
        assertEquals(Submarine.class.getSimpleName(), base.getName());
        assertEquals(4, counter.get());

        counter.reset();
        Ubot ubot = container.instance(Ubot.class).get();
        assertEquals("1", ubot.echo(1));
        assertEquals(42, ubot.getName());
        assertEquals(2, counter.get());
        Base<Integer> baseUbot = ubot;
        assertEquals("1", baseUbot.echo(1));
        assertEquals(42, ubot.getName());
        assertEquals(4, counter.get());

        counter.reset();
        Ponorka ponorka = container.instance(Ponorka.class).get();
        assertEquals("TRUE", ponorka.echo(true));
        assertNull(ponorka.getName());
        assertEquals(2, counter.get());
        Base<Boolean> basePonorka = ponorka;
        assertEquals("TRUE", basePonorka.echo(true));
        assertNull(basePonorka.getName());
        assertEquals(4, counter.get());

        counter.reset();
        NextSubmarine nextSubmarine = container.instance(NextSubmarine.class).get();
        assertEquals("foo", nextSubmarine.echo("foo"));
        assertEquals(NextSubmarine.class.getSimpleName(), nextSubmarine.getName());
        assertEquals(2, counter.get());
        // Now let's invoke the bridge method...
        NextBase<String> nextBase = nextSubmarine;
        assertEquals("foo", nextBase.echo("foo"));
        assertEquals(NextSubmarine.class.getSimpleName(), nextBase.getName());
        assertEquals(4, counter.get());
    }

    @Test
    public void testHierarchyWithInterface() {
        ArcContainer container = Arc.container();
        Counter counter = container.instance(Counter.class).get();

        counter.reset();
        ExampleResource exampleResource = container.instance(ExampleResource.class).get();
        assertEquals("foo", exampleResource.create("foo"));
        assertEquals(1, counter.get());

        counter.reset();
        ExampleApi exampleApi = container.instance(ExampleApi.class).get();
        assertEquals("foo", exampleApi.create("foo"));
        assertEquals(1, counter.get());
    }

    static class Base<T> {

        String echo(T payload) {
            return payload.toString().toUpperCase();
        }

        T getName() {
            return null;
        }

    }

    @ApplicationScoped
    static class Submarine extends Base<String> {

        @Simple
        @Override
        String echo(String payload) {
            return payload.toString();
        }

        @Simple
        @Override
        String getName() {
            return Submarine.class.getSimpleName();
        }

    }

    @Simple
    @ApplicationScoped
    static class Ubot extends Base<Integer> {

        @Override
        String echo(Integer payload) {
            return payload.toString();
        }

        @Override
        Integer getName() {
            return 42;
        }

    }

    @Simple
    @Singleton
    static class Ponorka extends Base<Boolean> {

    }

    static class NextBase<T extends Comparable<T>> {

        String echo(T payload) {
            return payload.toString().toUpperCase();
        }

        T getName() {
            return null;
        }

    }

    @ApplicationScoped
    static class NextSubmarine extends NextBase<String> {

        @Simple
        @Override
        String echo(String payload) {
            return payload.toString();
        }

        @Simple
        @Override
        String getName() {
            return NextSubmarine.class.getSimpleName();
        }

    }

}
