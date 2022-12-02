package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class CheckedTemplateBasePathTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Monks.class)
                    .addAsResource(new StringAsset("Hello {name}!"), "templates/foo/monk.txt")
                    .addAsResource(new StringAsset("Hello {name}!"), "templates/Monks/monk.txt"));

    @Test
    public void testBasePath() {
        assertEquals("Hello Ondrej!",
                Monks.Templates.monk("Ondrej").render());
        assertEquals("Hello Ondrej!",
                Monks.OtherTemplates.monk("Ondrej").render());
    }

}
