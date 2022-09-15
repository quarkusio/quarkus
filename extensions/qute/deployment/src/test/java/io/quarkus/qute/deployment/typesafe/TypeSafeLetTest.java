package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class TypeSafeLetTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Movie.class)
                    .addAsResource(new StringAsset("{@io.quarkus.qute.deployment.typesafe.Movie movie}"
                            + "{#let service=movie.findService('foo') name?=movie.name}"
                            + "{service.shortValue}"
                            + "::{name.length}"
                            + "{/let}"), "templates/foo.html"));

    @Inject
    Template foo;

    @Test
    public void testValidation() {
        assertEquals("10::5",
                foo.data("movie", new Movie()).render());
    }

}
