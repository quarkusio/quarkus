package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusExtensionTest;

public class LiteralBaseTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Movie.class)
                    .addAsResource(new StringAsset(
                            "{#let name=('foo' + 'bar') len='baz'.length price=(1 + 2)}"
                                    + "{name}::{len}::{price}"
                                    + "{/let}"),
                            "templates/literalBase.html")
                    .addAsResource(new StringAsset(
                            "{@io.quarkus.qute.deployment.typesafe.Movie movie}"
                                    + "{movie.findService('foo'.toUpperCase)}"),
                            "templates/literalBaseParam.html"));

    @Inject
    Template literalBase;

    @Inject
    Template literalBaseParam;

    @Test
    public void testLetWithLiteralBase() {
        assertEquals("foobar::3::3", literalBase.render());
    }

    @Test
    public void testLiteralBaseAsParam() {
        assertEquals("10", literalBaseParam.data("movie", new Movie()).render());
    }

}
