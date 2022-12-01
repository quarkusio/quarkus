package io.quarkus.qute.deployment.contenttypes;

import static io.quarkus.qute.TemplateInstance.SELECTED_VARIANT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.Variant;
import io.quarkus.test.QuarkusUnitTest;

public class AdditionalContentTypeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("Empty txt"), "templates/foo.txt")
                    .addAsResource(new StringAsset("Empty graphql"), "templates/foo.graphql")
                    .addAsResource(new StringAsset("quarkus.qute.content-types.graphql=application/graphql\n"
                            + "quarkus.qute.suffixes=txt,graphql"), "application.properties"));

    @Inject
    Template foo;

    @Test
    public void testVariant() {
        assertEquals("Empty graphql",
                foo.instance().setAttribute(SELECTED_VARIANT, Variant.forContentType("application/graphql")).render());
        assertEquals("Empty txt",
                foo.instance().setAttribute(SELECTED_VARIANT, Variant.forContentType("text/plain")).render());
        // foo.txt is used by default because it's first in the quarkus.qute.suffixes list
        assertEquals("Empty txt",
                foo.instance().render());
    }

}
