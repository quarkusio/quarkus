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

        Template tag = engine.parse("{#if showImage}{it.name}{#else}nope{/if}");
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

}
