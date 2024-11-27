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
        assertEquals("Čolek 1", json.escape("Čolek 1"));
        assertEquals("\\rČolek\\n", json.escape("\rČolek\n"));
        assertEquals("\\tČolek", json.escape("\tČolek"));
        assertEquals("\\\"tČolek", json.escape("\"tČolek"));
        assertEquals("\\\\tČolek", json.escape("\\tČolek"));
        assertEquals("\\\\u005C", json.escape("\\u005C"));
        assertEquals("\\u000bČolek", json.escape("\u000BČolek"));
        assertEquals("\\\\u000BČolek", json.escape("\\u000BČolek"));
        // Control char - start of Header
        assertEquals("\\u0001", json.escape("\u0001"));
    }

}
