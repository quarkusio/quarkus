package io.quarkus.micrometer.deployment.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.prometheus.metrics.expositionformats.generated.Metrics;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.test.QuarkusExtensionTest;

class PrometheusProtobufRuntimeInitializationTest {

    @SuppressWarnings("Convert2Lambda")
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.micrometer.export.prometheus.enabled", "true")
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .addBuildChainCustomizer(new Consumer<>() {
                @Override
                public void accept(BuildChainBuilder builder) {
                    builder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            // Derive the package from a generated message, independently of the processor's resource scan.
                            // Metrics exposes the generated types without hard-coding the shaded protobuf version.
                            String unsafeUtil = Metrics.MetricFamily.class.getSuperclass().getPackageName() + ".UnsafeUtil";
                            try {
                                Class.forName(unsafeUtil, false, Thread.currentThread().getContextClassLoader());
                            } catch (ClassNotFoundException e) {
                                throw new AssertionError("Prometheus protobuf UnsafeUtil must be loadable", e);
                            }
                            assertThat(context.consumeMulti(RuntimeInitializedClassBuildItem.class))
                                    .extracting(RuntimeInitializedClassBuildItem::getClassName)
                                    .contains(unsafeUtil);
                            context.produce(new FeatureBuildItem("prometheus-protobuf-runtime-initialization-test"));
                        }
                    }).consumes(RuntimeInitializedClassBuildItem.class).produces(FeatureBuildItem.class).build();
                }
            });

    @Test
    void registersShadedProtobufUnsafeUtilForRuntimeInitialization() {
        // The assertion runs in the augmentation classloader, where the build items are available.
    }
}
