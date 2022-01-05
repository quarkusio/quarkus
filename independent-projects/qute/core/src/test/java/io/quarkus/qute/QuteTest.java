package io.quarkus.qute;

import static io.quarkus.qute.Qute.fmt;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.qute.Qute.Fmt;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class QuteTest {

    @Test
    public void testQute() {
        Qute.setEngine(null);
        assertEquals("Hello Martin and Lu!", Qute.fmt("Hello {} and {}!", "Martin", "Lu"));
        assertEquals("Hello Martin!", Qute.fmt("Hello {name}!", Map.of("name", "Martin")));
        assertEquals("Animal name is cat!", Qute.fmt("Animal name is {animal.name}!", Map.of("animal", new Animal("cat"))));

        assertEquals("My name is Martin.", Qute.fmt("My name is {}.").cache().dataArray("Martin").render());
        assertEquals("My name is Martin.", fmt("My name is {data.0}.").noCache().dataArray("Martin").render());

        assertEquals("The name Lu is nice.",
                Qute.fmt("The name {name} is {#if name is 'Lu'}nice{#else}so-so{/if}.", Map.of("name", "Lu")));

        assertEquals("1::2::3::", Qute.fmt("{#each data.0}{it}::{/each}", List.of(1, 2, 3)));

        // set a different engine - this one does not use reflection but a custom value resolver for Animal
        Qute.setEngine(Engine.builder().addDefaults().addValueResolver(ValueResolver.builder().applyToBaseClass(Animal.class)
                .applyToName("name").applyToNoParameters().resolveSync(ec -> ((Animal) ec.getBase()).name).build()).build());

        assertEquals("Animal name is cat!", Qute.fmt("Animal name is {animal.name}!", Map.of("animal", new Animal("cat"))));
    }

    @Test
    public void testCustomVariant() {
        Qute.setEngine(Engine.builder()
                .addDefaults()
                .addParserHook(new Qute.IndexedArgumentsParserHook())
                .addResultMapper(new HtmlEscaper(List.of(Variant.TEXT_HTML)))
                .build());
        assertEquals("<br>", Qute.fmt("{}", "<br>"));
        assertEquals("&lt;br&gt;", Qute.fmt("{}").contentType("text/html").dataArray("<br>").render());
    }

    @Test
    public void testEngineReset() {
        Qute.setEngine(Engine.builder().build());
        Qute.setEngine(null);
        assertEquals("Foo", Qute.fmt("{data.0}", "Foo"));
    }

    @Test
    public void testLazyEvaluation() {
        Qute.setEngine(null);
        Fmt fmt = Qute.fmt("Lazy {animal.foo}!").data("animal", new Animal(null));
        assertEquals(0, Animal.FOO_COUNTER.get());
        assertEquals("Lazy 1!", fmt.toString());
        assertEquals(1, Animal.FOO_COUNTER.get());
    }

    @Test
    public void testAsync() {
        assertEquals("Hello Alpha!", Qute.fmt("Hello {}!").dataArray("Alpha").instance().createUni().await().indefinitely());
    }

    static class Animal {

        public final String name;

        static final AtomicInteger FOO_COUNTER = new AtomicInteger();

        public Animal(String name) {
            this.name = name;
        }

        public int getFoo() {
            return FOO_COUNTER.incrementAndGet();
        }

    }

}
