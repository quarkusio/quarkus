package io.quarkus.opentelemetry.runtime.graal;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.opentelemetry.api.events.EventEmitterProvider;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;

public class Substitutions {

    @TargetClass(className = "io.opentelemetry.sdk.autoconfigure.MeterProviderConfiguration")
    static final class Target_MeterProviderConfiguration {

        @Substitute
        static List<MetricReader> configureMetricReaders(
                ConfigProperties config,
                ClassLoader serviceClassLoader,
                BiFunction<? super MetricExporter, ConfigProperties, ? extends MetricExporter> metricExporterCustomizer,
                List<Closeable> closeables) {
            // OTel metrics not supported and there is no need to call
            // MetricExporterConfiguration.configurePrometheusMetricReader down the line.
            return Collections.emptyList();
        }
    }

    @TargetClass(className = "io.opentelemetry.sdk.autoconfigure.LoggerProviderConfiguration")
    static final class Target_LoggerProviderConfiguration {

        @Substitute
        static void configureLoggerProvider(
                SdkLoggerProviderBuilder loggerProviderBuilder,
                ConfigProperties config,
                ClassLoader serviceClassLoader,
                MeterProvider meterProvider,
                BiFunction<? super LogRecordExporter, ConfigProperties, ? extends LogRecordExporter> logRecordExporterCustomizer,
                List<Closeable> closeables) {
            // Logs not supported yet. No need to call LogRecordExporterConfiguration.configureExporter
        }
    }

    @TargetClass(className = "io.opentelemetry.api.events.GlobalEventEmitterProvider")
    static final class Target_GlobalEventEmitterProvider {
        @Substitute
        public static void set(EventEmitterProvider eventEmitterProvider) {
            // do nothing. We don't support events yet. Default is EventEmitterProvider.noop()
        }
    }
}
