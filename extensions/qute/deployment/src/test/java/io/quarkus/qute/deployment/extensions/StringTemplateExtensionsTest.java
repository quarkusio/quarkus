package io.quarkus.qute.deployment.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.Locale;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class StringTemplateExtensionsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addAsResource(
                            new StringAsset("{str:eval('Hello {name}!')}"),
                            "templates/hello.txt")
                    .addAsResource(
                            // https://github.com/quarkusio/quarkus/issues/47092
                            // This will trigger value resolver generation for StringBuilder
                            new StringAsset("{str:builder.append('Qute').append(\" is\").append(' cool!')}"),
                            "templates/builder.txt"));

    @Inject
    Engine engine;

    @Inject
    Template hello;

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
        assertEquals("barbar1",
                engine.parse("{foo + 'bar' + 1}")
                        .data("foo", "bar")
                        .render());
        assertEquals("barbar1",
                engine.parse("{str:concat(foo, 'bar', 1)}")
                        .data("foo", "bar")
                        .render());
        assertEquals("barbar1",
                engine.parse("{str:builder(foo).append('bar').append(1)}")
                        .data("foo", "bar")
                        .render());
        assertEquals("barbar1",
                engine.parse("{str:builder.append(foo).append('bar').append(1)}")
                        .data("foo", "bar")
                        .render());
        assertEquals("barbar1",
                engine.parse("{str:builder(foo) + 'bar' + 1}")
                        .data("foo", "bar")
                        .render());
        assertEquals("barbar1",
                engine.parse("{str:builder + foo + 'bar' + 1}")
                        .data("foo", "bar")
                        .render());
        assertEquals("Qute-is-cool",
                engine.parse("{str:join('-', 'Qute', 'is', foo)}")
                        .data("foo", "cool")
                        .render());
        assertEquals("Qute is cool!",
                engine.parse("{str:Qute + ' is ' + foo + '!'}")
                        .data("foo", "cool")
                        .render());
        assertEquals("Qute is cool!",
                engine.parse("{str:['Qute'] + ' is ' + foo + '!'}")
                        .data("foo", "cool")
                        .render());
        // note that this is not implemented as a template extension but a ns resolver
        assertEquals("Hello fool!",
                engine.parse("{str:eval('Hello {name}!')}")
                        .data("name", "fool")
                        .render());
        assertEquals("Hello fool!",
                hello.data("name", "fool")
                        .render());

        // https://github.com/quarkusio/quarkus/issues/47092
        assertEquals("Quteiscool!",
                engine.parse("{str:builder('Qute').append(\"is\").append(\"cool!\")}")
                        .render());
        assertEquals("Qute's cool!",
                engine.parse("{str:builder('Qute').append(\"'s\").append(\" cool!\")}")
                        .render());
        assertEquals("\"Qute\" is cool!",
                engine.parse("{str:builder('\"Qute\" ').append('is').append(\" cool!\")}")
                        .render());
        assertEquals("Hello '!",
                engine.parse("{str:concat(\"Hello '\",\"!\")}")
                        .render());

    }

}
