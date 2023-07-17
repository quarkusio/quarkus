package io.quarkus.qute.deployment.enums;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateData;
import io.quarkus.qute.TemplateEnum;
import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusUnitTest;

public class TemplateEnumIgnoredTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(TransactionType.class));

    @Inject
    Engine engine;

    @Test
    public void testTemplateData() {
        assertEquals("FOO",
                engine.parse("{io_quarkus_qute_deployment_enums_TemplateEnumIgnoredTest_TransactionType:FOO}").render());
        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(() -> engine.parse("{TransactionType:FOO}", null, "bar").render())
                .withMessage(
                        "Rendering error in template [bar] line 1: No namespace resolver found for [TransactionType] in expression {TransactionType:FOO}");

    }

    @TemplateEnum // ignored
    @TemplateData(namespace = TemplateData.UNDERSCORED_FQCN)
    public static enum TransactionType {

        FOO,
        BAR

    }

}
