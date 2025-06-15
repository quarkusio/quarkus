package io.quarkus.qute.deployment.enums;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateEnum;
import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusUnitTest;

public class TemplateEnumValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(TransactionType.class).addAsResource(
                    new StringAsset("{TransactionType:FOO}{TransactionType:BAR.scores}"), "templates/foo.txt"))
            .assertException(t -> {
                Throwable e = t;
                TemplateException te = null;
                while (e != null) {
                    if (e instanceof TemplateException) {
                        te = (TemplateException) e;
                        break;
                    }
                    e = e.getCause();
                }
                assertNotNull(te);
                assertTrue(te.getMessage().contains("Found incorrect expressions (1)"), te.getMessage());
                assertTrue(te.getMessage().contains("TransactionType:BAR.scores"), te.getMessage());
            });

    @Test
    public void test() {
        fail();
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
