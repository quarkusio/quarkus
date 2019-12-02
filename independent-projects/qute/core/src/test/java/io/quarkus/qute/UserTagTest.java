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
                .addSectionHelper(new UserTagSectionHelper.Factory("myTag"))
                .build();

        Template tag = engine.parse("{#if showImage}{it.name}{#else}nope{/if}");
        engine.putTemplate("myTag", tag);
        Template template1 = engine.parse("{#myTag order showImage=true /}");
        Template template2 = engine.parse("{#myTag order /}");
        Template template3 = engine.parse("{#myTag showImage=false /}");
        Template template4 = engine.parse("{#each this}{#myTag showImage=false /}{/each}");

        Map<String, Object> order = new HashMap<>();
        order.put("name", "Herbert");
        assertEquals("Herbert", template1.render(Collections.singletonMap("order", order)));
        assertEquals("nope", template2.render(Collections.singletonMap("order", order)));
        assertEquals("nope", template3.render(Collections.singletonMap("order", order)));
        assertEquals("nope", template4.render(Collections.singletonMap("order", order)));
    }

}
