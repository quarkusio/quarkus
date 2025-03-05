package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.quarkus.qute.EngineBuilder.EngineListener;
import io.quarkus.qute.TemplateInstance.Initializer;
import io.quarkus.qute.TemplateLocator.TemplateLocation;
import io.quarkus.qute.TemplateNode.Origin;

public class EngineTest {

    @Test
    public void testMapResut() {
        Engine engine = Engine.builder().addResultMapper((res, expr) -> "FOO").addResultMapper(new ResultMapper() {

            @Override
            public int getPriority() {
                // Is executed before the FOO mapper
                return 10;
            }

            @Override
            public boolean appliesTo(Origin origin, Object result) {
                return result instanceof Integer;
            }

            @Override
            public String map(Object result, Expression expression) {
                return "" + ((Integer) result) * 10;
            }
        }).build();
        Template test = engine.parse("{foo}");
        assertEquals("50",
                engine.mapResult(5, test.getExpressions().iterator().next()));
        assertEquals("FOO",
                engine.mapResult("bar", test.getExpressions().iterator().next()));
    }

    @Test
    public void testLocate() {
        assertEquals(Optional.empty(), Engine.builder().addDefaults().build().locate("foo"));
        Engine engine = Engine.builder().addDefaultSectionHelpers().addLocator(id -> Optional.of(new TemplateLocation() {

            @Override
            public Reader read() {
                return new StringReader("{foo}");
            }

            @Override
            public Optional<Variant> getVariant() {
                return Optional.empty();
            }

        })).build();
        Optional<TemplateLocation> location = engine.locate("foo");
        assertTrue(location.isPresent());
        try (Reader r = location.get().read()) {
            char[] buffer = new char[4096];
            StringBuilder b = new StringBuilder();
            int num;
            while ((num = r.read(buffer)) >= 0) {
                b.append(buffer, 0, num);
            }
            assertEquals("{foo}", b.toString());
        } catch (IOException e) {
            fail(e);
        }
    }

    @Test
    public void testNewBuilder() {
        Engine engine1 = Engine.builder()
                .addNamespaceResolver(NamespaceResolver.builder("foo").resolve(ec -> "baz").build())
                .addDefaults()
                .strictRendering(false)
                .useAsyncTimeout(false)
                .timeout(20_000)
                .addParserHook(new ParserHook() {
                    @Override
                    public void beforeParsing(ParserHelper parserHelper) {
                        parserHelper.addContentFilter(s -> s + "::{cool}");
                    }
                })
                .addTemplateInstanceInitializer(new Initializer() {
                    @Override
                    public void accept(TemplateInstance templateInstance) {
                        templateInstance.data("cool", true);
                    }
                })
                .build();
        assertEquals("foo::baz::true", engine1.parse("{ping}::{foo:whatever}").data("ping", "foo").render());
        assertFalse(engine1.getEvaluator().strictRendering());
        assertFalse(engine1.useAsyncTimeout());
        assertEquals(20_000, engine1.getTimeout());

        Engine engine2 = engine1.newBuilder()
                .useAsyncTimeout(true)
                .addValueResolver(
                        // This value resolver has the highest priority
                        ValueResolver.builder().applyToName("ping").priority(Integer.MAX_VALUE).resolveWith("pong").build())
                .build();

        assertEquals("pong::baz::true", engine2.parse("{ping}::{foo:whatever}").data("ping", "foo").render());
        assertEquals(20_000, engine2.getTimeout());
        assertFalse(engine2.getEvaluator().strictRendering());
        assertTrue(engine2.useAsyncTimeout());
        assertEquals(engine1.getSectionHelperFactories().size(), engine2.getSectionHelperFactories().size());
    }

    @Test
    public void testInvalidTemplateIdentifier() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Engine.builder().build().putTemplate("foo o", null));
        assertEquals("Invalid identifier found: [foo o]", e.getMessage());
    }

    private interface MyResolver extends ValueResolver, EngineListener {
    }

    private interface MyNamespaceResolver extends NamespaceResolver, EngineListener {
    }

    @Test
    public void testListeners() {
        AtomicReference<Engine> listenerEngine = new AtomicReference<>();
        Engine engine = Engine.builder()
                .timeout(2000)
                .addEngineListener(new EngineListener() {

                    @Override
                    public void engineBuilt(Engine engine) {
                        listenerEngine.set(engine);
                    }

                })
                .addValueResolver(new ReflectionValueResolver())
                .addValueResolver(new MyResolver() {

                    volatile Engine engine;

                    @Override
                    public CompletionStage<Object> resolve(EvalContext context) {
                        if (context.getName().equals("engine")) {
                            return CompletedStage.of(engine);
                        }
                        return Results.notFound();
                    }

                    @Override
                    public void engineBuilt(Engine engine) {
                        this.engine = engine;
                    }
                })
                .addNamespaceResolver(new MyNamespaceResolver() {

                    volatile Engine engine;

                    @Override
                    public CompletionStage<Object> resolve(EvalContext context) {
                        if (context.getName().equals("engine")) {
                            return CompletedStage.of(engine);
                        }
                        return Results.notFound();
                    }

                    @Override
                    public void engineBuilt(Engine engine) {
                        this.engine = engine;
                    }

                    @Override
                    public String getNamespace() {
                        return "foo";
                    }
                })
                .build();
        assertEquals(engine, listenerEngine.get());
        assertEquals("2000::2000", engine.parse("{engine.timeout}::{foo:engine.timeout}").render());
    }

}
