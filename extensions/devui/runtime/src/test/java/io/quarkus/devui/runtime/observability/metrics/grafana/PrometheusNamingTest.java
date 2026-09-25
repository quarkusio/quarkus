package io.quarkus.devui.runtime.observability.metrics.grafana;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * The expected names here were not derived from the specifications: they were read out of a running
 * Prometheus. A probe application registered a meter of every shape and was run three ways against the LGTM
 * Dev Service image (Micrometer with the Prometheus registry scraped, Micrometer bridged to OpenTelemetry,
 * and OpenTelemetry on its own), and the names Prometheus ended up holding are the ones asserted below.
 * <p>
 * The units passed in are the ones the Dev UI <em>captures</em>, which are not always the ones the meter
 * declares: {@code DevUiMetricsSampler} records a duration as {@code s} and a long task timer as
 * {@code tasks}, while Prometheus writes {@code seconds} into the name in both cases.
 */
public class PrometheusNamingTest {

    @Test
    public void micrometerAppendsTheBaseUnitAndTotal() {
        PrometheusNaming naming = PrometheusNaming.MICROMETER_PROMETHEUS;

        assertThat(naming.baseName("jvm.memory.used", "bytes", true)).isEqualTo("jvm_memory_used_bytes");
        assertThat(naming.baseName("jvm.threads.live", "threads", true)).isEqualTo("jvm_threads_live_threads");
        assertThat(naming.baseName("probe.queue.size", "items", true)).isEqualTo("probe_queue_size_items");
        assertThat(naming.counterName("probe.orders", null)).isEqualTo("probe_orders_total");
        assertThat(naming.counterName("probe.payload.received", "bytes"))
                .isEqualTo("probe_payload_received_bytes_total");
        // "s" as the Dev UI captures a duration, "seconds" as Prometheus spells it.
        assertThat(naming.baseName("probe.work", "s", false)).isEqualTo("probe_work_seconds");
        assertThat(naming.baseName("probe.work", "seconds", false)).isEqualTo("probe_work_seconds");
        assertThat(naming.baseName("probe.response.size", "bytes", false)).isEqualTo("probe_response_size_bytes");
    }

    @Test
    public void micrometerLeavesANameThatAlreadyEndsInItsUnitAlone() {
        PrometheusNaming naming = PrometheusNaming.MICROMETER_PROMETHEUS;

        assertThat(naming.baseName("jvm.memory.used.bytes", "bytes", true)).isEqualTo("jvm_memory_used_bytes");
        assertThat(naming.counterName("worker.pool.completed.total", null)).isEqualTo("worker_pool_completed_total");
    }

    @Test
    public void micrometerHasAMaxSeriesOfItsOwn() {
        assertThat(PrometheusNaming.MICROMETER_PROMETHEUS.maxName("probe.work", "s", true))
                .isEqualTo("probe_work_seconds_max");
        // A function timer tracks totals only, so there is no maximum to draw.
        assertThat(PrometheusNaming.MICROMETER_PROMETHEUS.maxName("probe.function.timer", "s", false)).isNull();
    }

    @Test
    public void aLongTaskTimerIsNamedAfterSecondsWhateverUnitWasCaptured() {
        // Captured as "tasks" (the active task count), published as probe_long_task_seconds_active_count.
        assertThat(PrometheusNaming.MICROMETER_PROMETHEUS.longTaskActiveName("probe.long.task", "tasks"))
                .isEqualTo("probe_long_task_seconds_active_count");
        // Over OTLP the meter arrives already split, so the active count is an ordinary meter.
        assertThat(PrometheusNaming.OTLP.longTaskActiveName("probe.long.task.active", "{tasks}"))
                .isEqualTo("probe_long_task_active");
    }

    @Test
    public void otlpSpellsTheUnitOut() {
        PrometheusNaming naming = PrometheusNaming.OTLP;

        assertThat(naming.baseName("jvm.memory.used", "By", true)).isEqualTo("jvm_memory_used_bytes");
        assertThat(naming.baseName("probe.native.duration", "s", false)).isEqualTo("probe_native_duration_seconds");
        assertThat(naming.baseName("http.server.requests", "ms", false))
                .isEqualTo("http_server_requests_milliseconds");
        assertThat(naming.baseName("probe.native.temperature", "Cel", true))
                .isEqualTo("probe_native_temperature_celsius");
    }

    @Test
    public void otlpDoesNotRepeatAUnitAlreadyInTheName() {
        PrometheusNaming naming = PrometheusNaming.OTLP;

        // The unit word is skipped wherever it appears in the name, not only at the end.
        assertThat(naming.baseName("jvm.threads.live", "threads", true)).isEqualTo("jvm_threads_live");
        assertThat(naming.baseName("process.files.open", "files", true)).isEqualTo("process_files_open");
        assertThat(naming.counterName("jvm.classes.unloaded", "classes")).isEqualTo("jvm_classes_unloaded_total");
        // ... but "buffer" is not "buffers", so this one does get the unit.
        assertThat(naming.baseName("jvm.buffer.count", "buffers", true)).isEqualTo("jvm_buffer_count_buffers");
    }

    @Test
    public void otlpDropsAnnotationsAndSpellsOutRates() {
        PrometheusNaming naming = PrometheusNaming.OTLP;

        assertThat(naming.counterName("probe.native.orders", "{order}")).isEqualTo("probe_native_orders_total");
        assertThat(naming.baseName("probe.native.active", "{task}", false)).isEqualTo("probe_native_active");
        assertThat(naming.counterName("probe.native.requests.rate", "{request}/s"))
                .isEqualTo("probe_native_requests_rate_per_second_total");
    }

    @Test
    public void otlpAddsRatioOnlyForGauges() {
        PrometheusNaming naming = PrometheusNaming.OTLP;

        assertThat(naming.baseName("jvm.cpu.recent_utilization", "1", true))
                .isEqualTo("jvm_cpu_recent_utilization_ratio");
        // "ratio" is a unit word like any other, so a name that already says it is left alone.
        assertThat(naming.baseName("probe.native.ratio", "1", true)).isEqualTo("probe_native_ratio");
        // jvm.cpu.limit is a non-monotonic sum, which Prometheus leaves without the ratio suffix.
        assertThat(naming.baseName("jvm.cpu.limit", "1", false)).isEqualTo("jvm_cpu_limit");
    }

    @Test
    public void otlpTakesTheMaximumFromACompanionMeter() {
        PrometheusNaming naming = PrometheusNaming.OTLP;

        assertThat(naming.maxName("http.server.requests", "ms", true))
                .isEqualTo("http_server_requests_max_milliseconds");
        // Nothing to draw when no maximum was captured, e.g. a plain OTel histogram.
        assertThat(naming.maxName("probe.native.duration", "s", false)).isNull();
    }

    @Test
    public void unknownUnitsAndCharactersAreLeftAsPrometheusLeavesThem() {
        assertThat(PrometheusNaming.OTLP.baseName("weird name/with:chars", "widgets", true))
                .isEqualTo("weird_name_with:chars_widgets");
        assertThat(PrometheusNaming.labelName("http.response.status")).isEqualTo("http_response_status");
    }

    @Test
    public void theNamingIsChosenByItsBuildTimeId() {
        assertThat(PrometheusNaming.fromId(PrometheusNaming.OTLP_ID)).isEqualTo(PrometheusNaming.OTLP);
        assertThat(PrometheusNaming.fromId(PrometheusNaming.MICROMETER_PROMETHEUS_ID))
                .isEqualTo(PrometheusNaming.MICROMETER_PROMETHEUS);
    }
}
