package io.quarkus.qute.deployment.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.Locale;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.test.QuarkusUnitTest;

public class StringTemplateExtensionsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication();

    @Inject
    Engine engine;

    @Test
    public void testTemplateExtensions() {
        assertEquals("hello:1",
                engine.parse("{str:format('%s:%s',greeting, 1)}").data("greeting", "hello").render());
        assertEquals("1",
                engine.parse("{str:fmt('%s',1)}").render());
        assertEquals(" d  c  b  a",
                engine.parse("{myStr.fmt('a','b','c','d')}").data("myStr", "%4$2s %3$2s %2$2s %1$2s").render());
        assertEquals("%s",
                engine.parse("{myStr.fmt(myStr)}").data("myStr", "%s").render());
        assertEquals("Hello Dorka!",
                engine.parse("{myStr.format(name)}").data("myStr", "Hello %s!", "name", "Dorka").render());
        assertEquals("Dienstag",
                engine.parse("{myStr.fmt(locale,now)}")
                        .data("myStr", "%tA", "now", LocalDateTime.of(2016, 7, 26, 12, 0), "locale", Locale.GERMAN)
                        .render());
        assertEquals("Dienstag",
                engine.parse("{str:fmt(locale,'%tA',now)}")
                        .data("now", LocalDateTime.of(2016, 7, 26, 12, 0), "locale", Locale.GERMAN)
                        .render());
    }

}
