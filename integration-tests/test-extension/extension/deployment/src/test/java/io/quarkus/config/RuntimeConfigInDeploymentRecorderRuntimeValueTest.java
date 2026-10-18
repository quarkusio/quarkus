package io.quarkus.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.extest.runtime.config.TestConfigRecorder;
import io.quarkus.extest.runtime.config.TestMappingRunTime;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.test.QuarkusExtensionTest;

public class RuntimeConfigInDeploymentRecorderRuntimeValueTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .addAdditionalDependency(ShrinkWrap.create(JavaArchive.class)
                    .addClass(TestConfigBuildStep.class)
                    .addAsResource(new StringAsset(TestConfigBuildStep.class.getName()),
                            "META-INF/quarkus-build-steps.list"))
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .assertException(throwable -> {
                assertInstanceOf(IllegalArgumentException.class, throwable);
                assertTrue(throwable.getMessage().startsWith("Unsupported method parameter"));
            });

    @Test
    void build() {

    }

    static class TestConfigBuildStep {
        @BuildStep
        @Record(value = ExecutionTime.STATIC_INIT)
        void step(TestConfigRecorder recorder, ConfigurationBuildItem configItem,
                RuntimeValue<TestMappingRunTime> runtimeConfig) {

        }
    }
}
