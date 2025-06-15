package io.quarkus.qute.deployment.enums;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateData;
import io.quarkus.qute.TemplateEnum;
import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusUnitTest;

public class TemplateEnumNamespaceValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(TransactionType.class, Transactions.class))
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
                assertTrue(te.getMessage().contains(
                        "The namespace [TransactionType] is defined by multiple @TemplateData and/or @TemplateEnum annotations"),
                        te.getMessage());
            });

    @Test
    public void test() {
        fail();
    }

    // namespace is TransactionType
    @TemplateEnum
    public static enum TransactionType {

        FOO,
        BAR

    }

    @TemplateData(namespace = "TransactionType")
    public static class Transactions {

    }

}
