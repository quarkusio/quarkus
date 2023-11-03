package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;

public class CheckedTemplateConflictTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Templates.class)
                    .addAsResource(new StringAsset("Hello {name}!"), "templates/CheckedTemplateConflictTest/monk.txt"))
            .setExpectedException(TemplateException.class);

    @Test
    public void testValidation() {
        fail();
    }

    @CheckedTemplate
    static class Templates {

        static native TemplateInstance monk(String name);

        static native TemplateInstance monk(Integer age);

    }

}
