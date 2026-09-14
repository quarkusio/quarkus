package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateData;
import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The aggregated validation error keeps one suppressed exception per incorrect expression so that tools can
 * read their origin, but those exceptions must not carry stack traces: they only add noise to the report.
 */
public class IncorrectExpressionsReportTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyClass.class)
                    .addAsResource(new StringAsset(
                            "{foo_My:BAZ} {foo_My:QUX}"),
                            "templates/foo.txt"))
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
                assertTrue(te.getMessage().contains("Found incorrect expressions (2)"), te.getMessage());
                assertEquals(2, te.getSuppressed().length);
                for (Throwable suppressed : te.getSuppressed()) {
                    TemplateException problem = assertInstanceOf(TemplateException.class, suppressed);
                    assertNotNull(problem.getOrigin());
                    assertEquals(0, problem.getStackTrace().length,
                            "the individual problems should not carry a stack trace");
                }
            });

    @Test
    public void test() {
        fail();
    }

    @TemplateData(namespace = "foo_My")
    public static class MyClass {

        public static final String FOO = "foo";

    }

}
