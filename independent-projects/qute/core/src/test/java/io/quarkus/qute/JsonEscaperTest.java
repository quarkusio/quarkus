package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.qute.TemplateNode.Origin;

public class JsonEscaperTest {

    @Test
    public void testAppliesTo() {
        JsonEscaper json = new JsonEscaper();
        Origin jsonOrigin = new Origin() {

            @Override
            public Optional<Variant> getVariant() {
                return Optional.of(Variant.forContentType(Variant.APPLICATION_JSON));
            }

            @Override
            public String getTemplateId() {
                return null;
            }

            @Override
            public String getTemplateGeneratedId() {
                return null;
            }

            @Override
            public int getLineCharacterStart() {
                return 0;
            }

            @Override
            public int getLineCharacterEnd() {
                return 0;
            }

            @Override
            public int getLine() {
                return 0;
            }
        };
        assertFalse(json.appliesTo(jsonOrigin, new RawString("foo")));
        assertTrue(json.appliesTo(jsonOrigin, "foo"));
    }

    @Test
    public void testEscaping() throws IOException {
        JsonEscaper json = new JsonEscaper();
        assertEquals("Čolek", json.escapeJson("Čolek"));
        assertEquals("\\rČolek\\n", json.escapeJson("\rČolek\n"));
        assertEquals("\\tČolek", json.escapeJson("\tČolek"));
        assertEquals("\\\"tČolek", json.escapeJson("\"tČolek"));
        assertEquals("\\\\tČolek", json.escapeJson("\\tČolek"));
        assertEquals("\\u000bČolek", json.escapeJson("\u000BČolek"));
    }

}
