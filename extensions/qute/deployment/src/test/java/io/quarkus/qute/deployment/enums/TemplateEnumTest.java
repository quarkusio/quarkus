package io.quarkus.qute.deployment.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Qute;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateEnum;
import io.quarkus.test.QuarkusUnitTest;

public class TemplateEnumTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TransactionType.class)
                    .addAsResource(new StringAsset(
                            "{#if tx == TransactionType:FOO}OK{/if}::{TransactionType:BAR}::{TransactionType:values[0]}"),
                            "templates/bar.txt"));

    @Inject
    Template bar;

    @Test
    public void testTemplateData() {
        assertEquals("OK::BAR::FOO", bar.data("tx", TransactionType.FOO).render());
        // Test the convenient Qute class
        assertEquals("OK", Qute.fmt("{#if tx == TransactionType:FOO}OK{#else}NOK{/if}", Map.of("tx", TransactionType.FOO)));
    }

    // namespace is TransactionType
    @TemplateEnum
    public static enum TransactionType {

        FOO,
        BAR

    }

}
