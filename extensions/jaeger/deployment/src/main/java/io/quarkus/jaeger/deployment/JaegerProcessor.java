package io.quarkus.jaeger.deployment;

import javax.inject.Inject;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.jaeger.runtime.JaegerBuildTimeConfig;
import io.quarkus.jaeger.runtime.JaegerConfig;
import io.quarkus.jaeger.runtime.JaegerDeploymentRecorder;
import io.quarkus.jaeger.runtime.QuarkusJaegerMetricsFactory;
import io.quarkus.runtime.ApplicationConfig;
import io.quarkus.smallrye.metrics.deployment.spi.MetricBuildItem;

public class JaegerProcessor {

    @Inject
    BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport;

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupTracer(JaegerDeploymentRecorder jdr, JaegerBuildTimeConfig buildTimeConfig, JaegerConfig jaeger,
            ApplicationConfig appConfig, Capabilities capabilities, BuildProducer<MetricBuildItem> metricProducer) {

        // Indicates that this extension would like the SSL support to be enabled
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.JAEGER));

        if (buildTimeConfig.enabled) {
            boolean metricsEnabled = capabilities.isCapabilityPresent(Capabilities.METRICS)
                    && buildTimeConfig.metricsEnabled;
            if (metricsEnabled) {
                produceMetrics(metricProducer);
                jdr.registerTracerWithMetrics(jaeger, appConfig);
            } else {
                jdr.registerTracerWithoutMetrics(jaeger, appConfig);
            }
        }
    }

    @BuildStep
    public void build(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.JAEGER));
    }

    @BuildStep
    public void reflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(ReflectiveClassBuildItem
                .builder("io.jaegertracing.internal.samplers.http.SamplingStrategyResponse",
                        "io.jaegertracing.internal.samplers.http.ProbabilisticSamplingStrategy")
                .finalFieldsWritable(true)
                .build());
    }

    private void produceMetrics(BuildProducer<MetricBuildItem> producer) {
        producer.produce(
                metric("jaeger_tracer_baggage_restrictions_updates", MetricType.COUNTER, null, new Tag("result", "err")));
        producer.produce(
                metric("jaeger_tracer_baggage_restrictions_updates", MetricType.COUNTER, null, new Tag("result", "ok")));
        producer.produce(metric("jaeger_tracer_baggage_truncations", MetricType.COUNTER, null));
        producer.produce(metric("jaeger_tracer_baggage_updates", MetricType.COUNTER, null, new Tag("result", "err")));
        producer.produce(metric("jaeger_tracer_baggage_updates", MetricType.COUNTER, null, new Tag("result", "ok")));
        producer.produce(metric("jaeger_tracer_finished_spans", MetricType.COUNTER, null));
        producer.produce(metric("jaeger_tracer_reporter_spans", MetricType.COUNTER, null, new Tag("result", "dropped")));
        producer.produce(metric("jaeger_tracer_reporter_spans", MetricType.COUNTER, null, new Tag("result", "err")));
        producer.produce(metric("jaeger_tracer_reporter_spans", MetricType.COUNTER, null, new Tag("result", "ok")));
        producer.produce(metric("jaeger_tracer_sampler_queries", MetricType.COUNTER, null, new Tag("result", "err")));
        producer.produce(metric("jaeger_tracer_sampler_queries", MetricType.COUNTER, null, new Tag("result", "ok")));
        producer.produce(metric("jaeger_tracer_sampler_updates", MetricType.COUNTER, null, new Tag("result", "ok")));
        producer.produce(metric("jaeger_tracer_sampler_updates", MetricType.COUNTER, null, new Tag("result", "err")));
        producer.produce(metric("jaeger_tracer_span_context_decoding_errors", MetricType.COUNTER, null));
        producer.produce(metric("jaeger_tracer_started_spans", MetricType.COUNTER, null, new Tag("sampled", "n")));
        producer.produce(metric("jaeger_tracer_started_spans", MetricType.COUNTER, null, new Tag("sampled", "y")));
        producer.produce(metric("jaeger_tracer_traces", MetricType.COUNTER, null,
                new Tag("sampled", "y"), new Tag("state", "joined")));
        producer.produce(metric("jaeger_tracer_traces", MetricType.COUNTER, null,
                new Tag("sampled", "y"), new Tag("state", "started")));
        producer.produce(metric("jaeger_tracer_traces", MetricType.COUNTER, null,
                new Tag("sampled", "n"), new Tag("state", "joined")));
        producer.produce(metric("jaeger_tracer_traces", MetricType.COUNTER, null,
                new Tag("sampled", "n"), new Tag("state", "started")));
        producer.produce(
                metric("jaeger_tracer_reporter_queue_length", MetricType.GAUGE, new QuarkusJaegerMetricsFactory.JaegerGauge()));
    }

    private MetricBuildItem metric(String name, MetricType type, Object implementor, Tag... tags) {
        Metadata metadata = Metadata.builder()
                .withName(name)
                .withDisplayName(name)
                .withType(type)
                .withUnit("none")
                .withDescription(name)
                .reusable()
                .build();
        if (implementor == null) {
            return new MetricBuildItem.Builder()
                    .metadata(metadata)
                    .tags(tags)
                    .build();
        } else {
            return new MetricBuildItem.Builder()
                    .metadata(metadata)
                    .implementor(implementor)
                    .tags(tags)
                    .build();
        }
    }

}
