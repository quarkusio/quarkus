package io.quarkus.qute;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Tests based on the description from
 * <a href="https://github.com/quarkusio/quarkus/issues/44674">https://github.com/quarkusio/quarkus/issues/44674</a>.
 */
public class LetTimeoutTest {

    Engine engine = Engine.builder().addDefaults().build();

    private static final Map<String, Object> DATA = Map.of(
            "a", Map.of("b", Map.of()));

    @Test
    void withDataFactoryMethod() {
        TemplateInstance instance = engine.parse("""
                {#let b = a.b}
                    {c}
                {/let}
                """).data(DATA);

        assertThatThrownBy(instance::render)
                .isInstanceOf(TemplateException.class)
                .hasRootCauseMessage("Rendering error: Key \"c\" not found in the map with keys [a] in expression {c}");
    }

    @Test
    void withInstanceThenDataForEachEntry() {
        TemplateInstance instance = engine.parse("""
                {#let b=a.b}
                    {c}
                {/let}
                """).instance();
        for (var e : DATA.entrySet()) {
            instance.data(e.getKey(), e.getValue());
        }
        assertThatThrownBy(instance::render)
                .isInstanceOf(TemplateException.class)
                .hasRootCauseMessage(
                        "Rendering error: Key \"c\" not found in the template data map with keys [a] in expression {c}");
    }

    @Test
    void withSet_withInstanceThenDataForEachEntry() {
        TemplateInstance instance = engine.parse("""
                {#set b = a.b}
                    {c}
                {/set}
                """).instance();
        for (var e : DATA.entrySet()) {
            instance.data(e.getKey(), e.getValue());
        }
        assertThatThrownBy(instance::render)
                .isInstanceOf(TemplateException.class)
                .hasRootCauseMessage(
                        "Rendering error: Key \"c\" not found in the template data map with keys [a] in expression {c}");
    }

    @Test
    void withLetWithoutEndTagwithInstanceThenDataForEachEntry() {
        TemplateInstance instance = engine.parse("""
                {#let b = a.b}
                    {c}
                """).instance();
        for (var e : DATA.entrySet()) {
            instance.data(e.getKey(), e.getValue());
        }
        assertThatThrownBy(instance::render)
                .isInstanceOf(TemplateException.class)
                .hasRootCauseMessage(
                        "Rendering error: Key \"c\" not found in the template data map with keys [a] in expression {c}");
    }
}
