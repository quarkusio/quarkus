package io.quarkus.qute;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

public class LoopSectionTest {

    @Test
    public void tesLoop() {

        Map<String, String> item = new HashMap<>();
        item.put("name", "Lu");

        List<Map<String, String>> items = new ArrayList<>();
        items.add(item);
        items.add(new HashMap<>());

        Engine engine = Engine.builder().addDefaults().build();

        Template template = engine
                .parse("{#for item in this}{item_count}.{item.name ?: 'NOT_FOUND'}{#if item_hasNext}\n{/if}{/for}");
        assertEquals("1.Lu\n2.NOT_FOUND", template.render(items));

        template = engine.parse("{#each this}{it_count}.{it.name ?: 'NOT_FOUND'}{#if it_hasNext}\n{/if}{/each}");
        assertEquals("1.Lu\n2.NOT_FOUND",
                template.render(items));

        template = engine.parse("{#each this}{#if it_odd}odd{#else}even{/if}{/each}");
        assertEquals("oddeven",
                template.render(items));
    }

    @Test
    public void testMapEntrySet() {
        Map<String, String> map = new HashMap<>();
        map.put("name", "Lu");

        Engine engine = Engine.builder()
                .addSectionHelper(new LoopSectionHelper.Factory()).addDefaultValueResolvers()
                .build();

        assertEquals("name:Lu", engine.parse("{#each this}{it.key}:{it.value}{/each}").render(map));
    }

    @Test
    public void testStream() {
        List<String> data = new ArrayList<>();
        data.add("alpha");
        data.add("bravo");
        data.add("charlie");

        Engine engine = Engine.builder().addDefaults().build();

        assertEquals("alpha:charlie:",
                engine.parse("{#each this}{it}:{/each}").render(data.stream().filter(e -> !e.startsWith("b"))));
    }

    @Test
    public void testNestedLoops() {
        List<String> list = new ArrayList<>();
        list.add("alpha");

        Engine engine = Engine.builder()
                .addSectionHelper(new LoopSectionHelper.Factory())
                .addSectionHelper(new IfSectionHelper.Factory())
                .addDefaultValueResolvers()
                .addValueResolver(new ValueResolver() {

                    public boolean appliesTo(EvalContext context) {
                        return ValueResolver.matchClass(context, String.class);
                    }

                    @Override
                    public CompletionStage<Object> resolve(EvalContext context) {
                        List<Character> chars = new ArrayList<>();
                        for (char c : context.getBase().toString().toCharArray()) {
                            chars.add(c);
                        }
                        return CompletedStage.of(chars);
                    }
                })
                .build();

        String template = "{#for name in list}"
                + "{name_count}.{name}: {#for char in name.chars}"
                + "{name} {global} char at {char_index} = {char}{#if char_hasNext},{/}"
                + "{/}{/}";

        assertEquals(
                "1.alpha: alpha - char at 0 = a,alpha - char at 1 = l,alpha - char at 2 = p,alpha - char at 3 = h,alpha - char at 4 = a",
                engine.parse(template).data("global", "-").data("list", list).render());
    }

    @Test
    public void testIntegerStream() {
        Engine engine = Engine.builder().addDefaults().build();

        assertEquals("1:2:3:",
                engine.parse("{#for i in total}{i}:{/for}").data("total", 3).render());
    }

    @Test
    public void testIterator() {
        Engine engine = Engine.builder().addDefaults().build();
        assertEquals("1:2:3:",
                engine.parse("{#for i in items}{i}:{/for}").data("items", Arrays.asList("1", "2", "3").iterator()).render());
    }

    @Test
    public void testArray() {
        Engine engine = Engine.builder().addDefaults().build();
        assertEquals("1:2:3:",
                engine.parse("{#for i in items}{i}:{/for}").data("items", new Integer[] { 1, 2, 3 }).render());
    }

    @Test
    public void testNull() {
        Engine engine = Engine.builder().addDefaults().build();
        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(() -> engine.parse("{#for i in items}{i}:{/for}").data("items", null).render())
                .withMessageContaining("{items} resolved to null, use {items.orEmpty} to ignore this error");
    }

    @Test
    public void testNoniterable() {
        Engine engine = Engine.builder().addDefaults().build();
        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(() -> engine.parse("{#for i in items}{i}:{/for}").data("items", Boolean.TRUE).render())
                .withMessageContaining("{items} resolved to [java.lang.Boolean] which is not iterable");
    }

    @Test
    public void testNotFound() {
        Engine engine = Engine.builder().addDefaults().strictRendering(false).build();
        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(() -> engine.parse("{#for i in items}{i}:{/for}").render())
                .withMessageContaining("{items} not found, use {items.orEmpty} to ignore this error");
    }

    @Test
    void testScope() {
        final HashMap<String, Object> dep1 = new HashMap<>();
        dep1.put("version", "1.0");
        final HashMap<String, Object> data = new HashMap<>();
        data.put("dependencies", Arrays.asList(dep1, new HashMap<>()));
        data.put("version", "hellllllo");
        Engine engine = Engine.builder().strictRendering(false).addDefaults().build();
        String result = engine.parse("{#for dep in dependencies}{#if dep.version}<version>{dep.version}</version>{/if}{/for}")
                .render(data);
        assertFalse(result.contains("hellllllo"), result);
        result = engine.parse("{#for dep in dependencies}{#if dep.version}<version>{version}</version>{/if}{/for}")
                .render(data);
        assertTrue(result.contains("hellllllo"), result);
        result = engine.parse("{#each dependencies}{#if it.version}<version>{it.version}</version>{/if}{/each}")
                .render(data);
        assertFalse(result.contains("hellllllo"), result);
        result = engine.parse("{#each dependencies}{#if it.version}<version>{version}</version>{/if}{/each}")
                .render(data);
        assertTrue(result.contains("hellllllo"), result);
    }

    @Test
    public void testElseBlock() {
        Engine engine = Engine.builder().addDefaults().build();
        assertEquals("No items.",
                engine.parse("{#for i in items}{item}{#else}No items.{/for}").data("items", Collections.emptyList()).render());
    }

    @Test
    public void testIterationMetadata() {
        String expected = "foo::0::1::false::true::true::odd::true::false";
        assertEquals(expected,
                Engine.builder().iterationMetadataPrefix(LoopSectionHelper.Factory.ITERATION_METADATA_PREFIX_NONE)
                        .addDefaults().build().parse(
                                "{#each items}{it}::{index}::{count}::{hasNext}::{isLast}::{isFirst}::{indexParity}::{odd}::{even}{/each}")
                        .data("items", List.of("foo")).render());
        assertEquals(expected,
                Engine.builder().iterationMetadataPrefix(LoopSectionHelper.Factory.ITERATION_METADATA_PREFIX_ALIAS_QM)
                        .addDefaults().build().parse(
                                "{#each items}{it}::{it?index}::{it?count}::{it?hasNext}::{it?isLast}::{it?isFirst}::{it?indexParity}::{it?odd}::{it?even}{/each}")
                        .data("items", List.of("foo")).render());
        assertEquals(expected,
                Engine.builder().addDefaults().build().parse(
                        "{#each items}{it}::{it_index}::{it_count}::{it_hasNext}::{it_isLast}::{it_isFirst}::{it_indexParity}::{it_odd}::{it_even}{/each}")
                        .data("items", List.of("foo")).render());
        assertEquals(expected,
                Engine.builder().iterationMetadataPrefix("meta_")
                        .addDefaults().build().parse(
                                "{#each items}{it}::{meta_index}::{meta_count}::{meta_hasNext}::{meta_isLast}::{meta_isFirst}::{meta_indexParity}::{meta_odd}::{meta_even}{/each}")
                        .data("items", List.of("foo")).render());
    }

}
