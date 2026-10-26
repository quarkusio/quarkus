package io.quarkus.opentelemetry.runtime.declarativeconfig;

import static io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusComponentLoaderTest.stubLoader;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.incubator.config.ConfigProvider;
import io.opentelemetry.api.incubator.config.DeclarativeConfigException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.YamlDeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.AutoConfigureListener;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ConfigurableMetricReaderProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ExtendedDeclarativeConfigProperties;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InternalTelemetryVersion;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.CollectionRegistration;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusDevUiMetricReaderProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusDropAllSpanProcessorProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusExtendedConfigProperties;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusPeriodicMetricReaderProvider;

class QuarkusPeriodicMetricReaderProviderTest {

    private static final ConfigProperties NO_PROPERTIES = DefaultConfigProperties.createFromMap(Map.of());

    @Test
    void exporterIsWrappedInAPeriodicReaderWithTheInterval() {
        NoopMetricExporter exporter = new NoopMetricExporter();
        Object reader = create(new QuarkusPeriodicMetricReaderProvider(null, UnaryOperator.identity()),
                loaderWith(exporterProvider("my-exporter", exporter)), "my-exporter", 5000);

        assertThat(reader).isInstanceOf(PeriodicMetricReader.class);
        assertThat(reader.toString()).contains("exporter=" + exporter, "intervalNanos=5000000000");
    }

    @Test
    void exporterCustomizerAppliesWithoutAnExecutor() {
        NoopMetricExporter customized = new NoopMetricExporter();
        Object reader = create(new QuarkusPeriodicMetricReaderProvider(null, exporter -> customized),
                loaderWith(exporterProvider("my-exporter", new NoopMetricExporter())), "my-exporter", 5000);

        assertThat(reader.toString()).contains("exporter=" + customized);
    }

    @Test
    void exporterCustomizerDoesNotApplyOnTheSharedExecutor() {
        // As on main: Quarkus's replacement reader exported through the exporter as it was before the customizers ran
        NoopMetricExporter exporter = new NoopMetricExporter();
        ScheduledThreadPoolExecutor shared = new ScheduledThreadPoolExecutor(1);
        try {
            Object reader = create(new QuarkusPeriodicMetricReaderProvider(shared, e -> new NoopMetricExporter()),
                    loaderWith(exporterProvider("my-exporter", exporter)), "my-exporter", 5000);

            assertThat(reader.toString()).contains("exporter=" + exporter);
        } finally {
            shared.shutdownNow();
        }
    }

    @Test
    void exporterReceivesExtendedProperties() {
        // Upstream components such as the OTLP exporters reject plain properties, and nested nodes are plain
        AtomicReference<DeclarativeConfigProperties> received = new AtomicReference<>();
        ComponentProvider provider = new ComponentProvider() {
            @Override
            public Class<?> getType() {
                return MetricExporter.class;
            }

            @Override
            public String getName() {
                return "extended-only";
            }

            @Override
            public Object create(DeclarativeConfigProperties config) {
                if (!(config instanceof ExtendedDeclarativeConfigProperties)) {
                    throw new IllegalArgumentException("Expected ExtendedDeclarativeConfigProperties");
                }
                received.set(config);
                return new NoopMetricExporter();
            }
        };

        // the SDK ConfigProvider upstream passes to the reader reaches the exporter
        ConfigProvider sdkConfigProvider = new ConfigProvider() {
            @Override
            public DeclarativeConfigProperties getInstrumentationConfig() {
                return DeclarativeConfigProperties.empty();
            }
        };
        ComponentLoader loader = loaderWith(provider);
        new QuarkusPeriodicMetricReaderProvider(null, UnaryOperator.identity()).create(
                new QuarkusExtendedConfigProperties(YamlDeclarativeConfigProperties.create(
                        Map.of("exporter", Map.of("extended-only", Map.of()), "interval", 5000), loader),
                        sdkConfigProvider));

        assertThat(received.get()).isInstanceOf(ExtendedDeclarativeConfigProperties.class);
        assertThat(((ExtendedDeclarativeConfigProperties) received.get()).getConfigProvider())
                .isSameAs(sdkConfigProvider);
    }

    @Test
    void exporterIsClosedWhenTheReaderCannotBeBuilt() {
        NoopMetricExporter exporter = new NoopMetricExporter();
        QuarkusPeriodicMetricReaderProvider provider = new QuarkusPeriodicMetricReaderProvider(null, e -> {
            throw new IllegalStateException("customizer failure");
        });

        assertThatThrownBy(() -> create(provider, loaderWith(exporterProvider("my-exporter", exporter)),
                "my-exporter", 5000)).hasMessage("customizer failure");
        assertThat(exporter.shutdown).isTrue();
    }

    @Test
    void quarkusReaderNamesAreNotMetricExporterNames() {
        RegistrationCapturingMetricReader devUiReader = new RegistrationCapturingMetricReader();
        ComponentLoader loader = new QuarkusComponentLoader(stubLoader(Map.of()), NO_PROPERTIES,
                List.of(new QuarkusDevUiMetricReaderProvider(devUiReader)));

        assertThatThrownBy(() -> create(new QuarkusPeriodicMetricReaderProvider(null, UnaryOperator.identity()),
                loader, QuarkusOTelModelBuilder.QUARKUS_DEVUI, 5000))
                .isInstanceOf(DeclarativeConfigException.class)
                .hasMessageContaining("No MetricExporter named 'quarkus_devui'");
    }

    @Test
    void configurableMetricReaderResolvesFromTheModelAndReceivesTheSdk() {
        // The whole path: model builder -> stand-in -> quarkus_periodic -> adapted ConfigurableMetricReaderProvider
        RegistrationCapturingMetricReader reader = new RegistrationCapturingMetricReader();
        AtomicReference<OpenTelemetrySdk> listened = new AtomicReference<>();
        class ListeningReaderProvider implements ConfigurableMetricReaderProvider, AutoConfigureListener {
            @Override
            public MetricReader createMetricReader(ConfigProperties config) {
                return reader;
            }

            @Override
            public String getName() {
                return "test-reader";
            }

            @Override
            public void afterAutoConfigure(OpenTelemetrySdk sdk) {
                listened.set(sdk);
            }
        }
        ComponentLoader loader = new QuarkusComponentLoader(
                stubLoader(Map.of(ConfigurableMetricReaderProvider.class, () -> List.of(new ListeningReaderProvider()))),
                NO_PROPERTIES, List.of(new QuarkusPeriodicMetricReaderProvider(null, UnaryOperator.identity()),
                        new QuarkusDropAllSpanProcessorProvider()));
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "none",
                "quarkus.otel.logs.exporter", "none",
                "quarkus.otel.metrics.exporter", "test-reader"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                loader, InternalTelemetryVersion.LATEST, Resource.getDefault())) {
            sdk.getMeter("test").counterBuilder("test.counter").build().add(1);

            assertThat(reader.registration.collectAllMetrics()).extracting(MetricData::getName)
                    .containsExactly("test.counter");
            assertThat(listened.get()).isSameAs(sdk);
        }
    }

    @Test
    void readerRunsOnAViewOfTheSharedExecutor() {
        AtomicInteger scheduled = new AtomicInteger();
        ScheduledThreadPoolExecutor shared = new ScheduledThreadPoolExecutor(1) {
            @Override
            public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
                scheduled.incrementAndGet();
                return super.schedule(command, delay, unit);
            }

            @Override
            public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period,
                    TimeUnit unit) {
                scheduled.incrementAndGet();
                return super.scheduleAtFixedRate(command, initialDelay, period, unit);
            }
        };
        try {
            MetricReader reader = (MetricReader) create(
                    new QuarkusPeriodicMetricReaderProvider(shared, UnaryOperator.identity()),
                    loaderWith(exporterProvider("my-exporter", new NoopMetricExporter())), "my-exporter", 60000);
            SdkMeterProvider meterProvider = SdkMeterProvider.builder().registerMetricReader(reader).build();

            assertThat(scheduled).hasPositiveValue();
            // shutting the reader down closes its view only, never the container-managed pool
            meterProvider.shutdown().join(10, TimeUnit.SECONDS);
            assertThat(shared.isShutdown()).isFalse();
        } finally {
            shared.shutdownNow();
        }
    }

    @Test
    void nameNoExporterClaimsResolvesAMetricReader() {
        // As autoconfigure: a name no metric exporter claims is looked up as a metric reader, e.g. prometheus
        RegistrationCapturingMetricReader reader = new RegistrationCapturingMetricReader();
        ConfigurableMetricReaderProvider provider = new ConfigurableMetricReaderProvider() {
            @Override
            public MetricReader createMetricReader(ConfigProperties config) {
                return reader;
            }

            @Override
            public String getName() {
                return "test-reader";
            }
        };
        ComponentLoader loader = new QuarkusComponentLoader(
                stubLoader(Map.of(ConfigurableMetricReaderProvider.class, () -> List.of(provider))), NO_PROPERTIES,
                List.of());

        Object created = create(new QuarkusPeriodicMetricReaderProvider(null, UnaryOperator.identity()), loader,
                "test-reader", 5000);
        assertThat(created).isSameAs(reader);

        try (SdkMeterProvider meterProvider = SdkMeterProvider.builder().registerMetricReader(reader).build()) {
            meterProvider.get("test").counterBuilder("test.counter").build().add(1);
            assertThat(reader.registration.collectAllMetrics()).extracting(MetricData::getName)
                    .containsExactly("test.counter");
        }
    }

    @Test
    void unknownNameFails() {
        assertThatThrownBy(() -> create(new QuarkusPeriodicMetricReaderProvider(null, UnaryOperator.identity()),
                loaderWith(), "missing", 5000))
                .isInstanceOf(DeclarativeConfigException.class)
                .hasMessageContaining("No MetricExporter named 'missing'");
    }

    private static Object create(QuarkusPeriodicMetricReaderProvider provider, ComponentLoader loader, String name,
            int intervalMillis) {
        DeclarativeConfigProperties config = YamlDeclarativeConfigProperties.create(
                Map.of("exporter", Map.of(name, Map.of()), "interval", intervalMillis), loader);
        return provider.create(config);
    }

    private static ComponentLoader loaderWith(ComponentProvider... providers) {
        return new QuarkusComponentLoader(stubLoader(Map.of(ComponentProvider.class, () -> List.of(providers))),
                NO_PROPERTIES, List.of());
    }

    private static ComponentProvider exporterProvider(String name, MetricExporter exporter) {
        return new ComponentProvider() {
            @Override
            public Class<?> getType() {
                return MetricExporter.class;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public Object create(DeclarativeConfigProperties config) {
                return exporter;
            }
        };
    }

    private static final class NoopMetricExporter implements MetricExporter {

        boolean shutdown;

        @Override
        public CompletableResultCode export(Collection<MetricData> metrics) {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            shutdown = true;
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
            return AggregationTemporality.CUMULATIVE;
        }
    }

    private static final class RegistrationCapturingMetricReader implements MetricReader {

        CollectionRegistration registration;

        @Override
        public void register(CollectionRegistration registration) {
            this.registration = registration;
        }

        @Override
        public CompletableResultCode forceFlush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
            return AggregationTemporality.CUMULATIVE;
        }
    }
}
