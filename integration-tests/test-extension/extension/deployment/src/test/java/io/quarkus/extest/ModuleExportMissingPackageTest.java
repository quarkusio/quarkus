package io.quarkus.extest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.builditem.ModuleExportBuildItem;
import io.quarkus.test.QuarkusExtensionTest;

public class ModuleExportMissingPackageTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withEmptyApplication()
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue()
                    && record.getLoggerName().startsWith("io.quarkus.deployment.jvm"))
            .assertLogRecords(records -> assertThat(records)
                    .as("Missing packages should not generate module reconfiguration warnings")
                    .isEmpty())
            .addBuildChainCustomizer(builder -> builder.addBuildStep(context -> context.produce(
                    new ModuleExportBuildItem("java.base", "no.such.target.module", "no.such.package", "jdk.internal.misc")))
                    .produces(ModuleExportBuildItem.class).build());

    @Test
    void startsWithMissingPackageInExistingModuleWithoutWarnings() {
        // Successful startup and assertLogRecords verify that Quarkus starts without module reconfiguration warnings.
        if (Runtime.version().feature() >= 25) {
            // Ignoring a missing package must not discard valid packages from the same build item.
            assertThat(Object.class.getModule().isExported("jdk.internal.misc", getClass().getModule())).isTrue();
        }
    }
}
