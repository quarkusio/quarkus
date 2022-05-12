package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class UserTagTest {

    @Test
    public void testUserTag() {
        Engine engine = Engine.builder().addDefaultSectionHelpers().addDefaultValueResolvers()
                .addSectionHelper(new UserTagSectionHelper.Factory("myTag", "my-tag-id"))
                .build();

        Template tag = engine.parse("{#if showImage.or(false)}{it.name}{#else}nope{/if}");
        // showImage.or(false), it.name
        assertEquals(2, tag.getExpressions().size());
        engine.putTemplate("my-tag-id", tag);

        Map<String, Object> order = new HashMap<>();
        order.put("name", "Herbert");
        assertEquals("Herbert",
                engine.parse("{#myTag order showImage=true /}").render(Collections.singletonMap("order", order)));
        assertEquals("nope", engine.parse("{#myTag order /}").render(Collections.singletonMap("order", order)));
        assertEquals("nope", engine.parse("{#myTag showImage=false /}").render(Collections.singletonMap("order", order)));
        assertEquals("nope",
                engine.parse("{#each this}{#myTag showImage=false /}{/each}").render(Collections.singletonMap("order", order)));
        assertEquals("Herbert",
                engine.parse("{#each this}{#myTag it showImage=true /}{/each}").render(Collections.singletonList(order)));
    }

    @Test
    public void testUserTagWithNestedContent() {
        Engine engine = Engine.builder().addDefaultSectionHelpers().addDefaultValueResolvers()
                .addSectionHelper(new UserTagSectionHelper.Factory("myTag", "my-tag-id"))
                .build();

        Template tag = engine.parse("{#if showImage.or(false)}<b>{#insert /}</b>{#else}nope{/if}");
        engine.putTemplate("my-tag-id", tag);

        Map<String, Object> order = new HashMap<>();
        order.put("name", "Herbert");
        assertEquals("<b>Herbert</b>",
                engine.parse("{#myTag showImage=true}{order.name}{/}").render(Collections.singletonMap("order", order)));
        assertEquals("nope", engine.parse("{#myTag}{order.name}{/}").render(Collections.singletonMap("order", order)));
        assertEquals("nope",
                engine.parse("{#myTag showImage=false}{order.name}{/}").render(Collections.singletonMap("order", order)));
        assertEquals("nope",
                engine.parse("{#each this}{#myTag showImage=false}{it.name}{/}{/each}")
                        .render(Collections.singletonMap("order", order)));
        assertEquals("<b>Herbert</b>",
                engine.parse("{#each this}{#myTag showImage=true}{it.name}{/}{/each}")
                        .render(Collections.singletonList(order)));
    }

    @Test
    public void testUserTagWithRichNestedContent() {
        Engine engine = Engine.builder().addDefaultSectionHelpers().addDefaultValueResolvers()
                .addSectionHelper(new UserTagSectionHelper.Factory("myTag", "my-tag-id"))
                .addSectionHelper(new UserTagSectionHelper.Factory("myTag2", "my-tag-id2"))
                .build();

        Template tag = engine.parse("{#if showImage}<b>{nested-content}</b>{#else}nope{/if}");
        engine.putTemplate("my-tag-id", tag);

        Template tag2 = engine.parse("{#if showImage2}<i>{nested-content}</i>{#else}nope2{/if}");
        engine.putTemplate("my-tag-id2", tag2);

        Map<String, Object> order = new HashMap<>();
        order.put("name", "Herbert");
        assertEquals("<b><i>Herbert</i></b>",
                engine.parse("{#myTag showImage=true}{#myTag2 showImage2=true}{order.name}{/myTag2}{/myTag}")
                        .render(Collections.singletonMap("order", order)));
    }

    @Test
    public void testUserTagLoopParam() {
        Engine engine = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver())
                .addSectionHelper(new UserTagSectionHelper.Factory("myTag", "my-tag-id"))
                .build();

        Template tag = engine.parse("{it} {surname}");
        engine.putTemplate("my-tag-id", tag);

        assertEquals("KOUBA kouba",
                engine.parse("{#each surnames}{#myTag it.toUpperCase surname=it.toLowerCase /}{/each}")
                        .data("surnames", Collections.singleton("Kouba")).render());
        assertEquals("KOUBA kouba",
                engine.parse(
                        "{#for surname in surnames}{#each surnames}{#myTag it.toUpperCase surname=surname.toLowerCase /}{/each}{/for}")
                        .data("surnames", Collections.singleton("Kouba")).render());
    }

    @Test
    public void testEval() {
        Engine engine = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver())
                .addSectionHelper(new UserTagSectionHelper.Factory("itemDetail", "my-tag-id"))
                .build();

        Template tag = engine.parse("{#set item=items.get(itemId)}{#eval myNestedContent item=item /}{/set}");
        engine.putTemplate("my-tag-id", tag);

        assertEquals("10 kg",
                engine.parse("{#itemDetail itemId=1 myNestedContent=\"{item.quantity} {item.unit}\" /}")
                        .data("items", Map.of(1, Map.of("quantity", 10, "unit", "kg"))).render());
    }

    @Test
    public void testDefaultedKey() {
        Engine engine = Engine.builder()
                .addDefaults()
                .addSectionHelper(new UserTagSectionHelper.Factory("myTag", "my-tag-id"))
                .strictRendering(false)
                .build();

        Template tag = engine.parse("{it}:{name}:{isCool}:{age}:{foo.bar}:{foo}");
        engine.putTemplate("my-tag-id", tag);
        assertEquals("Ondrej:Ondrej:true:2:NOT_FOUND:NOT_FOUND",
                engine.parse("{#myTag name age=2 isCool foo.length _isolated=true/}")
                        .data("name", "Ondrej", "isCool", true, "foo", "bzzz").render());
        assertEquals("Ondrej:Ondrej:true:2:NOT_FOUND:NOT_FOUND",
                engine.parse("{#myTag name age=2 isCool foo.length _isolated /}")
                        .data("name", "Ondrej", "isCool", true, "foo", "bzzz").render());
    }

    @Test
    public void testInsertSections() {
        Engine engine = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver())
                .addSectionHelper(new UserTagSectionHelper.Factory("myTag1", "mytag1"))
                .addSectionHelper(new UserTagSectionHelper.Factory("myTag2", "mytag2"))
                .build();
        engine.putTemplate("mytag1", engine.parse("{#insert foo}No foo!{/insert}::{#insert bar}No bar!{/insert}"));
        engine.putTemplate("mytag2", engine.parse("{#insert}Default content{/insert}"));

        assertEquals("Baz!::No bar!", engine.parse("{#myTag1 name='Baz'}{#foo}{name}!{/foo}{/myTag1}").render());
        assertEquals("Baz!", engine.parse("{#myTag2}Baz!{/myTag2}").render());
    }

}
