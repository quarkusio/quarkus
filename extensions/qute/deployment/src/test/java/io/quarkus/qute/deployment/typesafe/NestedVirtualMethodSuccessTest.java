package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class NestedVirtualMethodSuccessTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Movie.class, MovieExtensions.class)
                    .addAsResource(new StringAsset("{@io.quarkus.qute.deployment.typesafe.Movie movie}"
                            + "{movie.findServices(movie.name,movie.toNumber(movie.getName))}"),
                            "templates/nested.html"));

    @Inject
    Template nested;

    @Test
    public void testResult() {
        assertEquals("11", nested.data("movie", new Movie("John")).render());
    }

}
