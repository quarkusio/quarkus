package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class TypeSafeJavaLangTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Movie.class)
                    .addAsResource(new StringAsset("{@String movie}"
                            + "{@java.util.List<Integer> movies}"
                            + "{#if movie.toLowerCase is 'foo'}"
                            + "Foo movie!"
                            + "{/if}"
                            + "::{movies.get(0).intValue}"
                            + ""), "templates/foo.html"));

    @Inject
    Template foo;

    @Test
    public void testValidation() {
        assertEquals("Foo movie!::42",
                foo.data("movie", "foo", "movies", List.of(42)).render());
    }

}
