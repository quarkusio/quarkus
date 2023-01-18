package io.quarkus.qute.deployment.inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class NamedBeanIterableReturnTypeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClass(Validation.class)
                    .addAsResource(
                            new StringAsset(
                                    "{@java.lang.String field}"
                                            + "{#if cdi:validation.hasViolations(field)}"
                                            + "{#each cdi:validation.getViolations(field)}{it}{/each}"
                                            + "{/if}"),
                            "templates/validate.html"));

    @Inject
    Template validate;

    @Test
    public void testResult() {
        assertEquals("Foo!", validate.data("field", "foo").render());
    }

    @ApplicationScoped
    @Named
    public static class Validation {

        public boolean hasViolations(String field) {
            return true;
        }

        public List<String> getViolations(String field) {
            return List.of("Foo!");
        }
    }

}
