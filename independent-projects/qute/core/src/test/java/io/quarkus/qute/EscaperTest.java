package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.qute.TemplateNode.Origin;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * 
 */
public class EscaperTest {

    @Test
    public void testEscaping() throws IOException {
        Escaper escaper = Escaper.builder().add('a', "aaa").build();
        assertEquals("aaa", escaper.escape("a"));
        assertEquals("b", escaper.escape("b"));

        Escaper html = Escaper.builder().add('"', "&quot;").add('\'', "&#39;")
                .add('&', "&amp;").add('<', "&lt;").add('>', "&gt;").build();
        assertEquals("&lt;strong&gt;Čolek&lt;/strong&gt;", html.escape("<strong>Čolek</strong>"));
        assertEquals("&lt;a&gt;&amp;link&quot;&#39;&lt;/a&gt;", html.escape("<a>&link\"'</a>"));
    }

    @Test
    public void testRawStringRevolver() {

        Escaper escaper = Escaper.builder().add('a', "A").build();
        Engine engine = Engine.builder().addValueResolver(ValueResolvers.mapResolver())
                .addValueResolver(ValueResolvers.rawResolver()).addResultMapper(new ResultMapper() {

                    @Override
                    public boolean appliesTo(Origin origin, Object result) {
                        return result instanceof String;
                    }

                    @Override
                    public String map(Object result, Expression expression) {
                        return escaper.escape(result.toString());
                    }
                }).build();

        assertEquals("HAM HaM", engine.parse("{foo} {foo.raw}").data("foo", "HaM").render());

    }

}
