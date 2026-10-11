package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * An infix operand of an {@code if} section that refers to an unknown parameter is reported by the type-safe
 * validation as such, instead of the operator being treated as an expression on its own.
 */
public class IfSectionInfixOperandFailureTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Templates.class)
                    .addAsResource(new StringAsset("{#if index == count - foo}last{/if}"),
                            "templates/IfSectionInfixOperandFailureTest/last.txt"))
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
                assertTrue(te.getMessage().contains("foo"), te.getMessage());
                assertTrue(!te.getMessage().contains("{-}"), te.getMessage());
            });

    @Test
    public void test() {
        fail();
    }

    @CheckedTemplate
    public static class Templates {

        static native TemplateInstance last(int index, int count);

    }

}
