package io.quarkus.micrometer.runtime.produi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.inject.Instance;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.micrometer.runtime.produi.MicrometerProdUIService.MeterInfo;

/**
 * Unit tests for {@link MicrometerProdUIService#getMeters()} - in particular the numeric
 * {@code measurements} the Prod UI uses to plot charts alongside the formatted string value.
 */
class MicrometerProdUIServiceTest {

    @Test
    void returnsEmptyWhenNoRegistry() {
        @SuppressWarnings("unchecked")
        Instance<MeterRegistry> instance = mock(Instance.class);
        when(instance.isUnsatisfied()).thenReturn(true);

        MicrometerProdUIService service = serviceFor(instance);
        assertThat(service.getMeters()).isEmpty();
    }

    @Test
    void exposesNumericMeasurementsAlongsideFormattedValue() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Gauge.builder("jvm.memory.used", () -> 1024d)
                .tags(Tags.of("area", "heap", "id", "Eden"))
                .baseUnit("bytes")
                .register(registry);
        registry.counter("http.server.requests", "uri", "/hello").increment(3);

        MicrometerProdUIService service = serviceFor(satisfied(registry));
        List<MeterInfo> meters = service.getMeters();

        MeterInfo gauge = meters.stream().filter(m -> m.name().equals("jvm.memory.used")).findFirst().orElseThrow();
        assertThat(gauge.baseUnit()).isEqualTo("bytes");
        assertThat(gauge.value()).contains("value=1024");
        assertThat(gauge.measurements()).hasSize(1);
        assertThat(gauge.measurements().get(0).statistic()).isEqualTo("VALUE");
        assertThat(gauge.measurements().get(0).value()).isEqualTo(1024d);

        MeterInfo counter = meters.stream().filter(m -> m.name().equals("http.server.requests")).findFirst()
                .orElseThrow();
        assertThat(counter.measurements()).anySatisfy(measurement -> {
            assertThat(measurement.statistic()).isEqualTo("COUNT");
            assertThat(measurement.value()).isEqualTo(3d);
        });
    }

    @Test
    void skipsNonFiniteMeasurements() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Gauge.builder("system.cpu.usage", new AtomicInteger(), value -> Double.NaN).register(registry);

        MicrometerProdUIService service = serviceFor(satisfied(registry));
        MeterInfo meter = service.getMeters().stream()
                .filter(m -> m.name().equals("system.cpu.usage")).findFirst().orElseThrow();

        // The formatted string still records the raw NaN, but no NaN leaks into the numeric list (invalid JSON).
        assertThat(meter.value()).contains("NaN");
        assertThat(meter.measurements()).isEmpty();
    }

    private static Instance<MeterRegistry> satisfied(MeterRegistry registry) {
        @SuppressWarnings("unchecked")
        Instance<MeterRegistry> instance = mock(Instance.class);
        when(instance.isUnsatisfied()).thenReturn(false);
        when(instance.get()).thenReturn(registry);
        return instance;
    }

    private static MicrometerProdUIService serviceFor(Instance<MeterRegistry> instance) {
        MicrometerProdUIService service = new MicrometerProdUIService();
        service.registryInstance = instance;
        return service;
    }
}
