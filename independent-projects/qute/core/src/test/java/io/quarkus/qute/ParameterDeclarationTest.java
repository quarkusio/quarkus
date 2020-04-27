package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ParameterDeclarationTest {

    @Test
    public void testRemovedNewlinesAfterParameterDeclaration() {
        Engine engine = Engine.builder().addDefaults().build();

        Map<String, Object> data = new HashMap<>();
        data.put("name", "John");
        data.put("foo", "Bar");

        Template template = engine.parse(""
                + "{@java.lang.String name}\r\n"
                + "{@java.lang.String foo}\n"
                + "\n"
                + "{name}\n"
                + "{foo}\n");
        assertEquals("\nJohn\nBar\n", template.render(data));
    }

}
