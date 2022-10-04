package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.qute.TemplateNode.Origin;

public class HtmlEscaperTest {

    @Test
    public void testAppliesTo() {
        HtmlEscaper html = new HtmlEscaper(List.of(Variant.TEXT_HTML));
        Origin htmlOrigin = new Origin() {

            @Override
            public Optional<Variant> getVariant() {
                return Optional.of(Variant.forContentType(Variant.TEXT_HTML));
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
        assertFalse(html.appliesTo(htmlOrigin, new RawString("foo")));
        assertTrue(html.appliesTo(htmlOrigin, "foo"));
    }

    @Test
    public void testEscaping() throws IOException {
        HtmlEscaper html = new HtmlEscaper(List.of(Variant.TEXT_HTML));
        assertEquals("Čolek", html.escape("Čolek"));
        assertEquals("&lt;strong&gt;Čolek&lt;/strong&gt;", html.escape("<strong>Čolek</strong>"));
        assertEquals("&lt;a&gt;&amp;link&quot;&#39;&lt;/a&gt;", html.escape("<a>&link\"'</a>"));
    }

}
