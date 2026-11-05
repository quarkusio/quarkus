package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A value expression written with infix notation is a valid operand of an {@code if} section, also in a type-safe
 * template.
 */
public class IfSectionInfixOperandTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Templates.class)
                    .addAsResource(new StringAsset("{#if index == count - 1}last{#else}no{/if}"),
                            "templates/IfSectionInfixOperandTest/last.txt"));

    @Test
    public void testInfixOperand() {
        assertEquals("last", Templates.last(4, 5).render());
        assertEquals("no", Templates.last(3, 5).render());
    }

    @CheckedTemplate
    public static class Templates {

        static native TemplateInstance last(int index, int count);

    }

}
