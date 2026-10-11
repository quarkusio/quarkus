package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The implicit {@code it} of the calling template can be passed to a user tag as the first positional parameter,
 * e.g. {@code {#show it /}} inside a loop.
 */
public class UserTagItParamTest {

    @Test
    public void testItPassedAsFirstParam() {
        Engine engine = Engine.builder()
                .addDefaults()
                .addValueResolver(new ReflectionValueResolver())
                .addSectionHelper(new UserTagSectionHelper.Factory("show", "show-tag"))
                .addSectionHelper(new UserTagSectionHelper.Factory("named", "named-tag"))
                .strictRendering(true)
                .build();
        engine.putTemplate("show-tag", engine.parse("[{it}]"));
        engine.putTemplate("named-tag", engine.parse("[{val}]"));

        assertEquals("[1][2]", engine.parse("{#each items}{#show it /}{/each}").data("items", List.of(1, 2)).render());
        assertEquals("[1][2]", engine.parse("{#each items}{#named val=it /}{/each}").data("items", List.of(1, 2)).render());
        assertEquals("[42]", engine.parse("{#show foo /}").data("foo", 42).render());
        assertEquals("[42]", engine.parse("{#show it /}").data("it", 42).render());
    }

    @Test
    public void testNoFirstParam() {
        Engine engine = Engine.builder()
                .addDefaults()
                .addValueResolver(new ReflectionValueResolver())
                .addSectionHelper(new UserTagSectionHelper.Factory("show", "show-tag"))
                .strictRendering(false)
                .build();
        engine.putTemplate("show-tag", engine.parse("[{it ?: 'none'}]"));

        assertEquals("[none]", engine.parse("{#show /}").data("it", 42).render());
        assertEquals("[none]", engine.parse("{#show foo=1 /}").data("it", 42).render());
    }
}
