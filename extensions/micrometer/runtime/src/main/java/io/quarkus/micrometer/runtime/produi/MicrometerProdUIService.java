package io.quarkus.micrometer.runtime.produi;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Multi;

/**
 * Read-only metrics browser shared by Dev UI and Prod UI. It lists the meters
 * registered in the Micrometer {@link MeterRegistry} together with their current
 * values. Reading meters is non-destructive, so it is safe to expose in
 * production.
 */
@ApplicationScoped
public class MicrometerProdUIService {

    @Inject
    Instance<MeterRegistry> registryInstance;

    @NonBlocking
    @JsonRpcUsage({ Usage.DEV_UI, Usage.PROD_UI })
    @JsonRpcDescription("List all registered Micrometer meters with their current values")
    public List<MeterInfo> getMeters() {
        if (registryInstance.isUnsatisfied()) {
            return List.of();
        }
        MeterRegistry registry = registryInstance.get();
        List<MeterInfo> result = new ArrayList<>();
        for (Meter meter : registry.getMeters()) {
            Meter.Id id = meter.getId();
            String tags = id.getTags().stream()
                    .map(t -> t.getKey() + "=" + t.getValue())
                    .collect(Collectors.joining(", "));
            StringBuilder value = new StringBuilder();
            List<MeasurementValue> measurements = new ArrayList<>();
            for (Measurement measurement : meter.measure()) {
                String statistic = measurement.getStatistic().toString();
                double raw = measurement.getValue();
                if (value.length() > 0) {
                    value.append(", ");
                }
                value.append(statistic.toLowerCase())
                        .append('=')
                        .append(format(raw));
                // Keep a numeric copy alongside the formatted string so the UI can plot charts. Skip
                // NaN/Infinite values - they carry no chart meaning and are not valid JSON numbers.
                if (!Double.isNaN(raw) && !Double.isInfinite(raw)) {
                    measurements.add(new MeasurementValue(statistic, raw));
                }
            }
            result.add(new MeterInfo(id.getName(), id.getType().name(), tags,
                    id.getBaseUnit() == null ? "" : id.getBaseUnit(), value.toString(), measurements));
        }
        result.sort(Comparator.comparing(MeterInfo::name).thenComparing(MeterInfo::tags));
        return result;
    }

    @JsonRpcUsage({ Usage.DEV_UI, Usage.PROD_UI })
    @JsonRpcDescription("Stream all registered Micrometer meters, refreshed every few seconds")
    public Multi<List<MeterInfo>> streamMeters() {
        // Emit an immediate snapshot, then a fresh one on each tick, so the UI stays live without polling.
        // Reading meters is non-destructive, so streaming it is as safe as the one-shot getMeters().
        return Multi.createBy().concatenating().streams(
                Multi.createFrom().item(this::getMeters),
                Multi.createFrom().ticks().every(Duration.ofSeconds(2))
                        .onItem().transform(tick -> getMeters()));
    }

    private static String format(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return String.valueOf(value);
        }
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        return String.format("%.3f", value);
    }

    public record MeterInfo(String name, String type, String tags, String baseUnit, String value,
            List<MeasurementValue> measurements) {
    }

    public record MeasurementValue(String statistic, double value) {
    }
}
