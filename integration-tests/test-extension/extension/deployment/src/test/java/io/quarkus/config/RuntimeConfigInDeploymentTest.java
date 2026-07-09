package io.quarkus.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.extest.runtime.config.TestMappingRunTime;
import io.quarkus.test.QuarkusExtensionTest;

public class RuntimeConfigInDeploymentTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .addAdditionalDependency(ShrinkWrap.create(JavaArchive.class)
                    .addClass(ConfigInjectionBuildStep.class)
                    .addAsResource(new StringAsset(ConfigInjectionBuildStep.class.getName()),
                            "META-INF/quarkus-build-steps.list"))
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .assertException(throwable -> {
                assertInstanceOf(IllegalArgumentException.class, throwable);
                assertTrue(throwable.getMessage().startsWith("Run time configuration cannot be consumed"));
            });

    @Test
    void build() {

    }

    static class ConfigInjectionBuildStep {
        @Produce(ServiceStartBuildItem.class)
        @BuildStep
        void runtimeConfigInDeployment(ConfigurationBuildItem configItem, TestMappingRunTime runtimeConfig) {

        }
    }
}
