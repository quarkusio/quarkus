package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class TypeCheckExcludesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Movie.class, Machine.class, MachineStatus.class)
                    .addAsResource(new StringAsset("{@io.quarkus.qute.deployment.typesafe.Movie movie}"
                            + "{@io.quarkus.qute.deployment.typesafe.Machine machine}"
                            + "{movie.name}::{movie.superior}::{machine.ping}::{machine.neverEver}"), "templates/movie.html")
                    .addAsResource(new StringAsset(
                            "quarkus.qute.type-check-excludes=io.quarkus.qute.deployment.typesafe.Movie.superior,io.quarkus.qute.deployment.typesafe.Machine.*"
                                    + "\nquarkus.qute.strict-rendering=false"),
                            "application.properties"));

    @Inject
    Template movie;

    @Test
    public void testValidationSuccess() {
        assertEquals("Jason::NOT_FOUND::1::NOT_FOUND",
                movie.data("movie", new Movie(), "machine", new Machine()).render());
    }

}
