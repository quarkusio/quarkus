package io.quarkus.vertx.http.devconsole;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.qute.Engine;
import io.quarkus.vertx.http.deployment.devmode.console.DevConsoleProcessor.JavaDocResolver;

public class JavaDocResolverTest {

    @Test
    public void testResolver() {
        Engine engine = Engine.builder().addDefaults().addValueResolver(new JavaDocResolver()).build();
        assertEquals("<code>java.lang.Foo</code>::<code>Class#getSimpleName()</code>\n"
                + " <br><strong>@see</strong> Foo\n"
                + " <br><strong>@deprecated</strong> For some reason",
                engine.parse("{foo.fmtJavadoc}")
                        .data("foo",
                                "{@code java.lang.Foo}::{@link Class#getSimpleName()}\n @see Foo\n @deprecated For some reason")
                        .render().trim());

    }

}
