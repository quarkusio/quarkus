package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

public class TemplateExceptionTest {

    @Test
    public void testBuilder() {
        assertBuilder(TemplateException.builder().message("{} and {}").arguments("R", "J"), "R and J", null);
        assertBuilder(TemplateException.builder().message("{foo} and {bar}").argument("foo", "R").arguments(Map.of("bar", "J")),
                "R and J", null);
        assertBuilder(TemplateException.builder().message("OK").code(ParserError.EMPTY_EXPRESSION),
                "OK", ParserError.EMPTY_EXPRESSION.getName());
    }

    private void assertBuilder(TemplateException.Builder builder, String message, String codeName) {
        TemplateException e = builder.build();
        assertEquals(message, e.getMessage());
        if (codeName != null) {
            assertEquals(codeName, e.getCode().getName());
        }
    }

}
