package io.quarkus.qute.deployment.enums;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateEnum;
import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusUnitTest;

public class TemplateEnumInvalidTargetTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Transactions.class));

    @Inject
    Engine engine;

    @Test
    public void testTemplateEnum() {
        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(() -> engine.parse("{Transactions:VAL}", null, "bar").render())
                .withMessage(
                        "Rendering error in template [bar] line 1: No namespace resolver found for [Transactions] in expression {Transactions:VAL}");

    }

    @TemplateEnum // ignored
    public static class Transactions {

        public static final int VAL = 10;

    }

}
