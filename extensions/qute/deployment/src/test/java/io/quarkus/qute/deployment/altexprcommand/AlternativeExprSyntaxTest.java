package io.quarkus.qute.deployment.altexprcommand;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class AlternativeExprSyntaxTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addAsResource(new StringAsset("{=foo.toLowerCase}{ignored}"), "templates/testik.html"))
            .overrideConfigKey("quarkus.qute.alt-expr-syntax", "true");

    @Inject
    Template testik;

    @Test
    public void testAltSyntax() {
        assertEquals("clement{ignored}", testik.data("foo", "Clement").render());
    }

}
