package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class DefaultMethodValidationSuccessTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Movie.class, MovieExtensions.class)
                    .addAsResource(new StringAsset(
                            "{@io.quarkus.qute.deployment.typesafe.DefaultMethodValidationSuccessTest$Name name}Hello {name.fullName()}::{name.fullName}!"),
                            "templates/name.html"));

    @Inject
    Template name;

    @Test
    public void testResult() {
        // Validation succeeded! Yay!
        assertEquals("Hello Name Surname::Name Surname!", name.data("name", new Name()).render());
    }

    public static class Name implements Something {

        public String name() {
            return "Name";
        }
    }

    interface Something {

        String name();

        default String fullName() {
            return name() + " Surname";
        }
    }

}
