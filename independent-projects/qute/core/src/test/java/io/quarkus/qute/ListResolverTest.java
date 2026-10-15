package io.quarkus.qute;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class ListResolverTest {

    @Test
    public void tesResolver() {
        List<String> list = List.of("jedna", "dva", "tri");
        Engine engine = Engine.builder().addDefaults().build();
        assertEquals("3::jedna::jedna::dva::tri",
                engine.parse("{list.size}::{list.get(0)}::{list.take(1).0}::{list.takeLast(2).get(0)}::{list.2}")
                        .data("list", list).render());
        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(() -> engine.parse("{list.abc}").data("list", List.of()).render())
                .withMessageContaining(
                        "Property \"abc\" not found on the base object")
                .withMessageContaining("in expression {list.abc}");
    }

    @Test
    public void testIndexParameterIsRequired() {
        List<String> list = List.of("jedna", "dva", "tri");
        Engine engine = Engine.builder().addDefaults().build();
        for (String expression : List.of("{list.get}", "{list.take}", "{list.takeLast}")) {
            assertThatExceptionOfType(TemplateException.class)
                    .isThrownBy(() -> engine.parse(expression).data("list", list).render())
                    .withMessageContaining("not found on the base object")
                    .withMessageContaining("in expression " + expression);
        }
    }

}
