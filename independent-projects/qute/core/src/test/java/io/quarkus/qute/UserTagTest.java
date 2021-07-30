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

        Template tag = engine.parse("{#if showImage.or(false)}<b>{nested-content}</b>{#else}nope{/if}");
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

}
