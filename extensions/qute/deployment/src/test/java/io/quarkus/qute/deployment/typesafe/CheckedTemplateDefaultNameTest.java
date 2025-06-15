package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;

public class CheckedTemplateDefaultNameTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(UnderscoredTemplates.class)
                    .addAsResource(new StringAsset("Hello {val}!"),
                            "templates/CheckedTemplateDefaultNameTest/foo_bar.txt")
                    .addAsResource(new StringAsset("Olleh {val}!"), "templates/bim/foo-baz.txt"));

    @Test
    public void testBasePath() {
        assertEquals("Hello 1!", UnderscoredTemplates.fooBar(1).render());
        assertEquals("Olleh 42!", HyphenatedTemplates.fooBaz(42).render());
    }

    @CheckedTemplate(defaultName = CheckedTemplate.UNDERSCORED_ELEMENT_NAME)
    static class UnderscoredTemplates {

        static native TemplateInstance fooBar(int val);

    }

    @CheckedTemplate(basePath = "bim", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    static class HyphenatedTemplates {

        static native TemplateInstance fooBaz(int val);

    }

}
