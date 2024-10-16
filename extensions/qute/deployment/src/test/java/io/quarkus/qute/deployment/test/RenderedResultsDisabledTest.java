package io.quarkus.qute.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.RenderedResults;
import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class RenderedResultsDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(SimpleBean.class)
                    .addAsResource(new StringAsset("quarkus.qute.test-mode.record-rendered-results=false"),
                            "application.properties")
                    .addAsResource(new StringAsset("{name}"), "templates/foo.txt"));

    @Inject
    Instance<RenderedResults> renderedResults;

    @Inject
    Template foo;

    @Test
    public void testRenderedResultsNotRegistered() {
        assertTrue(renderedResults.isUnsatisfied());
        assertEquals("Morna", foo.data("name", "Morna").render());
    }

}
