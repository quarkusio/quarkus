package io.quarkus.opentelemetry.deployment.traces;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.opentelemetry.deployment.exporter.otlp.ExternalOtelExporterBuildItem;
import io.quarkus.opentelemetry.runtime.exporter.otlp.tracing.LateBoundSpanProcessor;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Verify that the default span exporter is not active when an external exporter is present.
 */
public class OpenTelemetryExternalExporterTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.traces.enabled", "true")
            .addBuildChainCustomizer(new Consumer<>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {
                    buildChainBuilder.addBuildStep().produces(ExternalOtelExporterBuildItem.class)
                            .setBuildStep(new BuildStep() {
                                @Override
                                public void execute(BuildContext context) {
                                    context.produce(new ExternalOtelExporterBuildItem("external-trace-exporter"));
                                }
                            }).build();
                }
            });

    @Inject
    InjectableInstance<LateBoundSpanProcessor> spanProcessorInstance;

    @Test
    void defaultExporterNotResolvable() throws Exception {
        assertThat(spanProcessorInstance.isResolvable()).isTrue();

        Field delegateField = LateBoundSpanProcessor.class.getDeclaredField("delegate");
        delegateField.setAccessible(true);

        Object obj = delegateField.get(spanProcessorInstance.get());
        assertThat(obj).isNull();
    }
}
