package io.quarkus.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.config.RuntimeConfigInDeploymentTest.ConfigInjectionBuildStep;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.extest.runtime.config.TestConfigRuntimeRecorder;
import io.quarkus.test.QuarkusExtensionTest;

public class RuntimeConfigInStaticInitTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .addAdditionalDependency(ShrinkWrap.create(JavaArchive.class)
                    .addClass(ConfigInjectionBuildStep.class)
                    .addAsResource(new StringAsset(ConfigInjectionBuildStep.class.getName()),
                            "META-INF/quarkus-build-steps.list"))
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .assertException(throwable -> {
                assertInstanceOf(IllegalArgumentException.class, throwable);
                assertTrue(
                        throwable.getMessage().contains("can only be injected in a @Recorder constructor as a RuntimeValue"));
            });

    @Test
    void build() {

    }

    static class ConfigInjectionBuildStep {
        @BuildStep
        @Record(ExecutionTime.STATIC_INIT)
        void runtimeConfigInDeployment(TestConfigRuntimeRecorder recorder, ConfigurationBuildItem configItem) {

        }
    }
}
