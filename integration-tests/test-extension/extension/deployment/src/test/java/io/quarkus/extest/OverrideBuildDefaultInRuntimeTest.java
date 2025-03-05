package io.quarkus.extest;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.test.QuarkusUnitTest;

public class OverrideBuildDefaultInRuntimeTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withEmptyApplication()
            .addBuildChainCustomizer(b -> {
                b.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new RunTimeConfigurationDefaultBuildItem("quarkus.log.console.enable", "false"));
                    }
                }).produces(RunTimeConfigurationDefaultBuildItem.class).build();
            });

    @Test
    public void testConsoleLogging() {
        assertFalse(ConfigProvider.getConfig().getValue("quarkus.log.console.enable", boolean.class));
    }
}
