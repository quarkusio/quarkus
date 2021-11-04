package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class ValidationSuccessTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Movie.class, MovieExtensions.class)
                    .addAsResource(new StringAsset("{@io.quarkus.qute.deployment.typesafe.Movie movie}"
                            + "{@java.lang.Long age}"
                            + "{@java.lang.String surname}"
                            + "{@java.util.Map<String,String map}"
                            // Property found
                            + "{movie.name} "
                            // Built-in value resolvers
                            + "{movie.name ?: 'Mono'} "
                            + "{movie.alwaysTrue ? 'Mono' : 'Stereo'} "
                            + "{movie.alwaysFalsePrimitive ? 'Mono' : 'Stereo'} "
                            + "{movie.alwaysFalsePrimitive.negate} "
                            + "{movie.mainCharacters.size} "
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
                            + "{#each movie.mainCharacters}{it.substring(1)}{/} "
                            // Method param assignability
                            + "{map.get('foo')}"),
                            "templates/movie.html"));

    @Inject
    Template movie;

    @Test
    public void testResult() {
        // Validation succeeded! Yay!
        assertEquals("Jason Jason Mono Stereo true 1 10 11 ok 43 3 ohn bar",
                movie.data("movie", new Movie("John"), "name", "Vasik", "surname", "Hu", "age", 10l, "map",
                        Collections.singletonMap("foo", "bar")).render());
    }

}
