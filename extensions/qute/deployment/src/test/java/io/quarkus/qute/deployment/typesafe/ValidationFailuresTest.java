package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusUnitTest;

public class ValidationFailuresTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Movie.class, MovieExtensions.class)
                    .addAsResource(new StringAsset("{@io.quarkus.qute.deployment.typesafe.Movie movie}"
                            + "{@java.lang.Long age}"
                            // Property not found
                            + "{movie.foo}"
                            // Name ok but incorrect number of parameters
                            + "{movie.getName('foo')}"
                            // Name and number of params ok; the parameter type does not match
                            + "{movie.findService(age)}"
                            + "{movie.findService(10l)} "
                            // Name and number of params ok; the parameter type does not match
                            + "{movie.findServices(age,name)}"
                            // Name and number of params ok for extension method; the parameter type does not match
                            + "{movie.toNumber(age)}"
                            + "{#each movie.mainCharacters}{it.boom(1)}{/}"
                            // Template extension method must accept one param
                            + "{movie.toNumber}"
                            + "{#each movie}{it}{/each}"
                            // Bean not found
                            + "{movie.findService(inject:ageBean)}"),
                            "templates/movie.html"))
            .assertException(t -> {
                Throwable e = t;
                TemplateException te = null;
                while (e != null) {
                    if (e instanceof TemplateException) {
                        te = (TemplateException) e;
                        break;
                    }
                    e = e.getCause();
                }
                assertNotNull(te);
                assertTrue(te.getMessage().contains("Found template problems (10)"), te.getMessage());
                assertTrue(te.getMessage().contains("movie.foo"), te.getMessage());
                assertTrue(te.getMessage().contains("movie.getName('foo')"), te.getMessage());
                assertTrue(te.getMessage().contains("movie.findService(age)"), te.getMessage());
                assertTrue(te.getMessage().contains("movie.findService(10l)"), te.getMessage());
                assertTrue(te.getMessage().contains("movie.findServices(age,name)"), te.getMessage());
                assertTrue(te.getMessage().contains("movie.toNumber(age)"), te.getMessage());
                assertTrue(te.getMessage().contains("it.boom(1)"), te.getMessage());
                assertTrue(te.getMessage().contains("movie.toNumber"), te.getMessage());
                assertTrue(te.getMessage().contains("inject:ageBean"), te.getMessage());
                assertTrue(
                        te.getMessage().contains("Unsupported iterable type found: io.quarkus.qute.deployment.typesafe.Movie"),
                        te.getMessage());
            });

    @Test
    public void test() {
        fail();
    }

}
