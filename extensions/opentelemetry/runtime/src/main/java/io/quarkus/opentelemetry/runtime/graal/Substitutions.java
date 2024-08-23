package io.quarkus.opentelemetry.runtime.graal;

import java.io.Closeable;
import java.util.List;
import java.util.function.BiFunction;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.opentelemetry.api.incubator.events.EventLoggerProvider;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.sdk.autoconfigure.internal.SpiHelper;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;

public class Substitutions {

    @TargetClass(className = "io.opentelemetry.sdk.autoconfigure.LoggerProviderConfiguration")
    static final class Target_LoggerProviderConfiguration {

        @Substitute
        static void configureLoggerProvider(
                SdkLoggerProviderBuilder loggerProviderBuilder,
                ConfigProperties config,
                SpiHelper spiHelper,
                MeterProvider meterProvider,
                BiFunction<? super LogRecordExporter, ConfigProperties, ? extends LogRecordExporter> logRecordExporterCustomizer,
                BiFunction<? super LogRecordProcessor, ConfigProperties, ? extends LogRecordProcessor> logRecordProcessorCustomizer,
                List<Closeable> closeables) {
            // Logs not supported yet. No need to call LogRecordExporterConfiguration.configureExporter
        }
    }

    @TargetClass(className = "io.opentelemetry.api.incubator.events.GlobalEventLoggerProvider")
    static final class Target_GlobalEventEmitterProvider {
        @Substitute
        public static void set(EventLoggerProvider eventEmitterProvider) {
            // do nothing. We don't support events yet. Default is EventEmitterProvider.noop()
        }
    }
}
