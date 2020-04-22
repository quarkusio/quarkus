package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class ValidationSuccessTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Movie.class, MovieExtensions.class)
                    .addAsResource(new StringAsset("{@io.quarkus.qute.deployment.typesafe.Movie movie}"
                            + "{@java.lang.Long age}"
                            + "{@java.lang.String surname}"
                            // Property found
                            + "{movie.name} "
                            // Built-in value resolvers
                            + "{movie.name ?: 'Mono'} "
                            + "{movie.alwaysTrue ? 'Mono' : 'Stereo'} "
                            // Name and number of params ok and param type ignored
                            + "{movie.findService('foo')} "
                            // Name and number of params ok; name type ignored, age ok
                            + "{movie.findServices(name,age)} "
                            // Varargs method
                            + "{movie.findNames(age,'foo',surname)} "
                            // Name, number of params and type ok for extension method
                            + "{movie.toNumber(surname)} "
                            // Varargs extension method
                            + "{movie.toLong(1l,2l)} "
                            // Field access
                            + "{#each movie.mainCharacters}{it.substring(1)}{/}"),
                            "templates/movie.html"));

    @Inject
    Template movie;

    @Test
    public void testResult() {
        // Validation succeeded! Yay!
        assertEquals("Jason Jason Mono 10 11 ok 43 3 ohn",
                movie.data("movie", new Movie("John")).data("name", "Vasik").data("surname", "Hu").data("age", 10l).render());
    }

}
