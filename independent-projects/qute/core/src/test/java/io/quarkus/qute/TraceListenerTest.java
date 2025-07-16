package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.qute.trace.ResolveEvent;
import io.quarkus.qute.trace.TemplateEvent;
import io.quarkus.qute.trace.TraceListener;

public class TraceListenerTest {

    private static final Comparator<String> NODE_COMPARATOR = (o1, o2) -> o1.compareTo(o2);

    @Test
    public void trackTemplate() {
        Engine engine = Engine.builder()
                .addDefaults()
                .addValueResolver(new ReflectionValueResolver())
                .enableTracing(true)
                .build();

        String templateId = "hello";
        Template template = engine.parse("""
                <html>
                   {#for item in items}
                   {#if item_count > 0}FOO{/if}
                       {item} {item_count}
                   {/for}
                </html>
                """, null, templateId);

        StringBuilder trace = new StringBuilder();
        engine.addTraceListener(new TraceListener() {

            @Override
            public void onStartTemplate(TemplateEvent event) {
                String templateId = event.getTemplateInstance().getTemplate().getId();
                trace.append("<").append(templateId).append(">");
            }

            @Override
            public void onEndTemplate(TemplateEvent event) {
                String templateId = event.getTemplateInstance().getTemplate().getId();
                trace.append("</").append(templateId).append(">");
            }

        });

        List<String> items = List.of("foo", "bar", "baz");
        template.data("items", items).render();

        assertEquals("<hello></hello>", trace.toString());
    }

    @Test
    public void trackNodes() {
        Engine engine = Engine.builder()
                .addDefaults()
                .addValueResolver(new ReflectionValueResolver())
                .enableTracing(true)
                .build();

        Template template = engine.parse("""
                <html>
                   {#for item in items}
                   {#if item_count > 0}FOO{/if}
                       {item} {item_count}
                   {/for}
                </html>
                """);

        List<String> actualBeforeResolve = new ArrayList<>();
        List<String> actualAfterResolve = new ArrayList<>();
        engine.addTraceListener(new TraceListener() {
            @Override
            public void onBeforeResolve(ResolveEvent event) {
                actualBeforeResolve.add(toStringNode(event.getTemplateNode()));
            }

            @Override
            public void onAfterResolve(ResolveEvent event) {
                actualAfterResolve.add(toStringNode(event.getTemplateNode()));
            }

        });

        List<String> items = List.of("foo", "bar", "baz");
        template.data("items", items).render();

        // Before and after resolve, nodes are equal but not necessarily in the same
        // order, as some nodes may take longer to resolve.
        List<String> sortedBeforeResolve = new ArrayList<>(actualBeforeResolve);
        List<String> sortedAfterResolve = new ArrayList<>(actualAfterResolve);
        Collections.sort(sortedBeforeResolve, NODE_COMPARATOR);
        Collections.sort(sortedAfterResolve, NODE_COMPARATOR);
        assertEquals(sortedAfterResolve, sortedAfterResolve);

        assertArrayEquals(expectedBeforeResolve().toArray(), actualBeforeResolve.toArray());
    }

    @Test
    public void testRegistration() {
        Engine engine = Engine.builder()
                .addDefaults()
                .addValueResolver(new ReflectionValueResolver())
                .enableTracing(true)
                .build();

        TraceListener empty = new TraceListener() {
        };
        engine.addTraceListener(empty);
        assertTrue(engine.getTraceManager().hasTraceListeners());
        engine.removeTraceListener(empty);
        assertFalse(engine.getTraceManager().hasTraceListeners());

        assertNull(Engine.builder().addDefaults().enableTracing(false).build().getTraceManager());
    }

    private static List<String> expectedBeforeResolve() {
        List<String> actual = new ArrayList<String>();
        actual.add("TextNode [value=<html>]");
        actual.add("SectionNode [helper=LoopSectionHelper, origin= ]");
        actual.add("TextNode [value=   ]");
        actual.add("SectionNode [helper=IfSectionHelper, origin= ]");
        actual.add("TextNode [value=FOO]");
        actual.add("TextNode [value=       ]");
        actual.add("ExpressionNode [expression=Expression [namespace=null, parts=[item], literal=null]]");
        actual.add("TextNode [value= ]");
        actual.add("ExpressionNode [expression=Expression [namespace=null, parts=[item_count], literal=null]]");
        actual.add("TextNode [value=]");
        actual.add("TextNode [value=   ]");
        actual.add("SectionNode [helper=IfSectionHelper, origin= ]");
        actual.add("TextNode [value=FOO]");
        actual.add("TextNode [value=       ]");
        actual.add("ExpressionNode [expression=Expression [namespace=null, parts=[item], literal=null]]");
        actual.add("TextNode [value= ]");
        actual.add("ExpressionNode [expression=Expression [namespace=null, parts=[item_count], literal=null]]");
        actual.add("TextNode [value=]");
        actual.add("TextNode [value=   ]");
        actual.add("SectionNode [helper=IfSectionHelper, origin= ]");
        actual.add("TextNode [value=FOO]");
        actual.add("TextNode [value=       ]");
        actual.add("ExpressionNode [expression=Expression [namespace=null, parts=[item], literal=null]]");
        actual.add("TextNode [value= ]");
        actual.add("ExpressionNode [expression=Expression [namespace=null, parts=[item_count], literal=null]]");
        actual.add("TextNode [value=]");
        actual.add("TextNode [value=</html>]");
        return actual;
    }

    private static String toStringNode(TemplateNode templateNode) {
        return templateNode.toString().replace("\r", "").replace("\n", "");
    }
}
