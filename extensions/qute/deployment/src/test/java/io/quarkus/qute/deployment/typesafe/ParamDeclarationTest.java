package io.quarkus.qute.deployment.typesafe;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class ParamDeclarationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(Movie.class)
                    .addAsResource(new StringAsset("{@io.quarkus.qute.deployment.typesafe.Movie movie}"
                            + "{movie.mainCharacters.size}: {#for character in movie.mainCharacters}"
                            + "{character}"
                            + "{#if hasNext}, {/}"
                            + "{/}"), "templates/movie.html"));

    @Inject
    Template movie;

    @Test
    public void testValidationSuccess() {
        Assertions.assertEquals("2: Michael Caine, John Cleese",
                movie.data("movie", new Movie("Michael Caine", "John Cleese")).render());
    }

}
