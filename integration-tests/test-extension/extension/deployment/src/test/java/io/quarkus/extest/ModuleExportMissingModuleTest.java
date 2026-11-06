package io.quarkus.extest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.builditem.ModuleExportBuildItem;
import io.quarkus.test.QuarkusExtensionTest;

public class ModuleExportMissingModuleTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withEmptyApplication()
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue()
                    && record.getLoggerName().startsWith("io.quarkus.deployment.jvm"))
            .assertLogRecords(records -> assertThat(records)
                    .as("Missing source and target modules should not generate module reconfiguration warnings")
                    .isEmpty())
            .addBuildChainCustomizer(builder -> builder.addBuildStep(context -> {
                context.produce(new ModuleExportBuildItem("no.such.source.module", "no.such.target.module", "no.such.package"));
                context.produce(new ModuleExportBuildItem("java.base", "no.such.target.module", "java.lang"));
            }).produces(ModuleExportBuildItem.class).build());

    @Test
    void startsWithMissingSourceAndTargetModulesWithoutWarnings() {
        // Successful startup and assertLogRecords verify that Quarkus starts without module reconfiguration warnings.
    }
}
