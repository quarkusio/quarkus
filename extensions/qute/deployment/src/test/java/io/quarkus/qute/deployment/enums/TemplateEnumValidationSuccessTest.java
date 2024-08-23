package io.quarkus.qute.deployment.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateEnum;
import io.quarkus.test.QuarkusUnitTest;

public class TemplateEnumValidationSuccessTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(TransactionType.class)
                    .addAsResource(new StringAsset(
                            "{TransactionType:FOO}::{TransactionType:BAR.score}"),
                            "templates/foo.txt"));

    @Inject
    Template foo;

    @Test
    public void testEnum() {
        assertEquals("FOO::42", foo.render());
    }

    @TemplateEnum
    public static enum TransactionType {

        FOO,
        BAR;

        public int getScore() {
            return 42;
        }

    }

}
