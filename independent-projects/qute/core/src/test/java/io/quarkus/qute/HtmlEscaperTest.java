package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.qute.TemplateNode.Origin;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class HtmlEscaperTest {

    @Test
    public void testAppliesTo() {
        HtmlEscaper html = new HtmlEscaper();
        Origin htmlOrigin = new Origin() {

            @Override
            public Optional<Variant> getVariant() {
                return Optional.of(new Variant(Locale.getDefault(), Variant.TEXT_HTML, null));
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
        HtmlEscaper html = new HtmlEscaper();
        assertEquals("Čolek", html.escape("Čolek"));
        assertEquals("&lt;strong&gt;Čolek&lt;/strong&gt;", html.escape("<strong>Čolek</strong>"));
        assertEquals("&lt;a&gt;&amp;link&quot;&#39;&lt;/a&gt;", html.escape("<a>&link\"'</a>"));
    }

}
