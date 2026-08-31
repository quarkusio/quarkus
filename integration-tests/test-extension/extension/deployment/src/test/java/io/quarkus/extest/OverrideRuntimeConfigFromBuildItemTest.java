package io.quarkus.extest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.config.Config;

public class OverrideRuntimeConfigFromBuildItemTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withEmptyApplication()
            .addBuildChainCustomizer(b -> {
                b.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new RunTimeConfigurationDefaultBuildItem("quarkus.mapping.rt.override-build-item",
                                "from-build-item"));
                    }
                }).produces(RunTimeConfigurationDefaultBuildItem.class).build();
            });

    @Test
    public void overrideRuntimeConfigFromBuildItem() {
        assertEquals("from-build-item", Config.get().getConfigValue("quarkus.mapping.rt.override-build-item").getValue());
    }
}
