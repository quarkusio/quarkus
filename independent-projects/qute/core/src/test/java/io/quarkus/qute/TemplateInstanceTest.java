package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import io.quarkus.qute.TemplateImpl.Capacity;

public class TemplateInstanceTest {

    @Test
    public void testInitializer() {
        Engine engine = Engine.builder()
                .addDefaults()
                .addTemplateInstanceInitializer(instance -> instance.data("foo", "bar").setAttribute("alpha", Boolean.TRUE))
                .build();

        Template hello = engine.parse("Hello {foo}!");
        TemplateInstance instance = hello.instance();
        assertEquals(Boolean.TRUE, instance.getAttribute("alpha"));
        assertEquals("Hello bar!", instance.render());
        instance.data("foo", "baz");
        assertEquals("Hello baz!", instance.render());
    }

    @Test
    public void testRendered() {
        Engine engine = Engine.builder().addDefaults().build();
        Template hello = engine.parse("Hello {foo}!");
        AtomicBoolean rendered = new AtomicBoolean();
        TemplateInstance instance = hello.instance().data("foo", "baz").onRendered(() -> rendered.set(true));
        assertEquals("Hello baz!", instance.render());
        assertTrue(rendered.get());
    }

    @Test
    public void testGetTemplate() {
        Engine engine = Engine.builder().addDefaults().build();
        Template hello = engine.parse("Hello {foo}!");
        String generatedId = hello.getGeneratedId();
        assertEquals(generatedId, hello.instance().getTemplate().getGeneratedId());
    }

    @Test
    public void testComputeData() {
        Engine engine = Engine.builder().addDefaults().build();
        TemplateInstance instance = engine.parse("Hello {foo} and {bar}!").instance();
        AtomicBoolean barUsed = new AtomicBoolean();
        AtomicBoolean fooUsed = new AtomicBoolean();
        instance
                .computedData("bar", key -> {
                    barUsed.set(true);
                    return key.length();
                })
                .data("bar", 30)
                .computedData("foo", key -> {
                    fooUsed.set(true);
                    return key.toUpperCase();
                });
        assertFalse(fooUsed.get());
        assertEquals("Hello FOO and 30!", instance.render());
        assertTrue(fooUsed.get());
        assertFalse(barUsed.get());
    }

    @Test
    public void testLocale() throws Exception {
        Engine engine = Engine.builder().addDefaults()
                .addValueResolver(ValueResolver.builder()
                        .applyToName("locale")
                        .resolveSync(ctx -> ctx.getAttribute(TemplateInstance.LOCALE))
                        .build())
                .build();
        Template hello = engine.parse("Hello {locale}!");
        assertEquals("Hello fr!", hello.instance().setLocale(Locale.FRENCH).render());
    }

    @Test
    public void testVariant() {
        Engine engine = Engine.builder().addDefaults()
                .addValueResolver(ValueResolver.builder()
                        .applyToName("variant")
                        .resolveSync(ctx -> ctx.getAttribute(TemplateInstance.SELECTED_VARIANT))
                        .build())
                .addValueResolver(ValueResolver.builder()
                        .appliesTo(ctx -> ctx.getBase() instanceof Variant && ctx.getName().equals("contentType"))
                        .resolveSync(ctx -> ((Variant) ctx.getBase()).getContentType())
                        .build())
                .build();
        Template hello = engine.parse("Hello {variant.contentType}!");
        String render = hello.instance().setVariant(Variant.forContentType(Variant.TEXT_HTML)).render();
        assertEquals("Hello text/html!", render);
    }

    @Test
    public void testCapacity() {
        Engine engine = Engine.builder().addDefaults().build();
        assertCapacity(engine, "foo", 3, 3, Map.of());
        assertCapacity(engine, "{! comment is ignored !}foo", 3, 3, Map.of());
        assertCapacity(engine, "{foo} and bar", 10 + 8, 28, Map.of("foo", "bazzz".repeat(4)));
        assertCapacity(engine, "{#each foo}bar{/}", 10 * 3, 3, Map.of("foo", List.of(1)));
        assertCapacity(engine, "{#include bar /} and bar", 500 + 8, -1, Map.of());
        // limit reached
        assertCapacity(engine, "{#each}{foo}{/}".repeat(1000), Capacity.LIMIT, -1, Map.of());
        assertCapacity(engine, "{foo}", 10, Capacity.LIMIT, Map.of("foo", "b".repeat(70_000)));
    }

    private void assertCapacity(Engine engine, String val, int expectedComputed, int expectedMax, Map<String, Object> data) {
        TemplateImpl template = (TemplateImpl) engine.parse(val);
        assertEquals(expectedComputed, template.capacity.computed);
        if (expectedMax != -1) {
            template.render(data);
            assertEquals(expectedMax, template.capacity.max);
        }
    }
}
