package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.quarkus.qute.RenderedResults.RenderedResult;

public class ResultsCollectingTemplateInstanceTest {

    @Test
    public void testRender() throws InterruptedException {
        assertResults(t -> t.render());
    }

    @Test
    public void testRenderAsync() throws InterruptedException {
        assertResults(t -> {
            try {
                return t.renderAsync().toCompletableFuture().get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testCreateUni() throws InterruptedException {
        assertResults(t -> t.createUni().await().indefinitely());
    }

    @Test
    public void testCreateMulti() throws InterruptedException {
        assertResults(t -> t.createMulti().subscribe().asStream().collect(Collectors.joining()));
    }

    @Test
    public void testConsume() throws InterruptedException {
        assertResults(t -> {
            try {
                StringBuilder builder = new StringBuilder();
                t.consume(builder::append).toCompletableFuture().get();
                return builder.toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testRemove() {
        Engine engine = Engine.builder().addDefaults().build();
        RenderedResults renderedResults = new RenderedResults();
        ResultsCollectingTemplateInstance collectingInstance = new ResultsCollectingTemplateInstance(
                engine.parse("{name}", null, "foo").instance().data("name", "oof"),
                renderedResults);
        assertEquals("oof", collectingInstance.render());
        renderedResults.remove(r -> r.result().contains("of"));
        assertTrue(renderedResults.getResults("foo").isEmpty());
    }

    @Test
    public void testIterator() {
        Engine engine = Engine.builder().addDefaults().build();
        RenderedResults renderedResults = new RenderedResults();
        ResultsCollectingTemplateInstance collectingInstance = new ResultsCollectingTemplateInstance(
                engine.parse("{name}", null, "foo").instance().data("name", "oof"),
                renderedResults);
        for (int i = 0; i < 10; i++) {
            assertEquals("oof", collectingInstance.render());
        }
        for (Entry<String, List<RenderedResult>> e : renderedResults) {
            assertEquals(e.getKey(), "foo");
            assertEquals(10, e.getValue().size());
        }
    }

    @Test
    public void testFilter() {
        Engine engine = Engine.builder().addDefaults().build();
        RenderedResults renderedResults = new RenderedResults();
        renderedResults.setFilter((t, r) -> r.templateId().equals("foo"));
        assertEquals("foo", new ResultsCollectingTemplateInstance(
                engine.parse("foo", null, "foo").instance(),
                renderedResults).render());
        assertEquals("bar", new ResultsCollectingTemplateInstance(
                engine.parse("bar", null, "bar").instance(),
                renderedResults).render());
        List<RenderedResult> fooResults = renderedResults.getResults("foo");
        assertEquals(1, fooResults.size());
        assertEquals("foo", fooResults.get(0).result());
        List<RenderedResult> barResults = renderedResults.getResults("bar");
        assertTrue(barResults.isEmpty());
    }

    private void assertResults(Function<TemplateInstance, String> renderAction) throws InterruptedException {
        Engine engine = Engine.builder().addDefaults().build();
        RenderedResults renderedResults = new RenderedResults();
        ResultsCollectingTemplateInstance collectingInstance = new ResultsCollectingTemplateInstance(
                engine.parse("{name}", null, "foo").instance().data("name", "oof"),
                renderedResults);
        assertEquals("foo", collectingInstance.getTemplate().getId());
        assertEquals("oof", renderAction.apply(collectingInstance));
        List<RenderedResult> results = renderedResults.getResults("foo");
        assertEquals(1, results.size());
        assertEquals("oof", results.get(0).result());
        // Wait a little so that we can test the RenderedResult#timeout()
        // Note that LocalDateTime.now() has precision of the system clock and it seems that windows has millisecond precision
        TimeUnit.MILLISECONDS.sleep(50);
        assertEquals("oof", renderAction.apply(collectingInstance));
        results = renderedResults.getResults("foo");
        assertEquals(2, results.size());
        assertEquals("oof", results.get(1).result());
        assertTrue(results.get(0).timestamp().isBefore(results.get(1).timestamp()));
        renderedResults.clear();
        assertTrue(renderedResults.getResults("foo").isEmpty());
    }

}
