package io.quarkus.observability.testcontainers;

import static io.opentelemetry.semconv.JvmAttributes.JVM_MEMORY_POOL_NAME;
import static io.opentelemetry.semconv.JvmAttributes.JVM_MEMORY_TYPE;
import static io.opentelemetry.semconv.JvmAttributes.JvmMemoryTypeValues.HEAP;
import static io.opentelemetry.semconv.JvmAttributes.JvmMemoryTypeValues.NON_HEAP;
import static io.opentelemetry.semconv.incubating.JvmIncubatingMetrics.JVM_MEMORY_COMMITTED_NAME;
import static io.opentelemetry.semconv.incubating.JvmIncubatingMetrics.JVM_MEMORY_COMMITTED_UNIT;
import static io.opentelemetry.semconv.incubating.JvmIncubatingMetrics.JVM_MEMORY_LIMIT_NAME;
import static io.opentelemetry.semconv.incubating.JvmIncubatingMetrics.JVM_MEMORY_LIMIT_UNIT;
import static io.opentelemetry.semconv.incubating.JvmIncubatingMetrics.JVM_MEMORY_USED_NAME;
import static io.opentelemetry.semconv.incubating.JvmIncubatingMetrics.JVM_MEMORY_USED_UNIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.yaml.snakeyaml.Yaml;

import io.opentelemetry.api.common.AttributeKey;

/**
 * Checks the JVM memory queries of the provisioned Grafana dashboards.
 * <p>
 * The "Quarkus Micrometer OpenTelemetry" dashboard must work with two sets of metric names:
 * <ul>
 * <li>Micrometer names, sent by the Micrometer OpenTelemetry bridge: {@code jvm_memory_max_bytes} and the
 * {@code area} and {@code id} labels.</li>
 * <li>OpenTelemetry semantic conventions, sent by the OpenTelemetry extension alone: {@code jvm_memory_limit_bytes}
 * and the {@code jvm_memory_type} and {@code jvm_memory_pool_name} labels.</li>
 * </ul>
 */
public class GrafanaDashboardsTest {

    private static final Dashboard OPENTELEMETRY_DASHBOARD = Dashboard
            .load("grafana-dashboard-quarkus-micrometer-opentelemetry.json");
    private static final Dashboard OTLP_DASHBOARD = Dashboard.load("grafana-dashboard-quarkus-micrometer-otlp.json");

    private static final String HEAP_POOL_VARIABLE = "jvm_memory_pool_heap";
    private static final String NON_HEAP_POOL_VARIABLE = "jvm_memory_pool_nonheap";

    private static final String MICROMETER_MEMORY_USED = "jvm_memory_used_bytes";
    private static final String MICROMETER_MEMORY_COMMITTED = "jvm_memory_committed_bytes";
    private static final String MICROMETER_MEMORY_MAX = "jvm_memory_max_bytes";
    private static final String MICROMETER_AREA = "area";
    private static final String MICROMETER_POOL = "id";
    private static final String MICROMETER_HEAP = "heap";
    private static final String MICROMETER_NON_HEAP = "nonheap";

    private static final String SEMCONV_MEMORY_USED = prometheusMetric(JVM_MEMORY_USED_NAME, JVM_MEMORY_USED_UNIT);
    private static final String SEMCONV_MEMORY_COMMITTED = prometheusMetric(JVM_MEMORY_COMMITTED_NAME,
            JVM_MEMORY_COMMITTED_UNIT);
    private static final String SEMCONV_MEMORY_LIMIT = prometheusMetric(JVM_MEMORY_LIMIT_NAME, JVM_MEMORY_LIMIT_UNIT);
    private static final String SEMCONV_MEMORY_TYPE = prometheusLabel(JVM_MEMORY_TYPE);
    private static final String SEMCONV_POOL = prometheusLabel(JVM_MEMORY_POOL_NAME);

    static Stream<Arguments> openTelemetryDashboardMemoryQueries() {
        return Stream.of(
                arguments("Heap used", "A",
                        byArea(MICROMETER_MEMORY_USED, MICROMETER_HEAP), byType(SEMCONV_MEMORY_USED, HEAP)),
                arguments("Heap used", "A",
                        byArea(MICROMETER_MEMORY_MAX, MICROMETER_HEAP), byType(SEMCONV_MEMORY_LIMIT, HEAP)),
                arguments("Non-Heap used", "A",
                        byArea(MICROMETER_MEMORY_USED, MICROMETER_NON_HEAP), byType(SEMCONV_MEMORY_USED, NON_HEAP)),
                arguments("Non-Heap used", "A",
                        byArea(MICROMETER_MEMORY_MAX, MICROMETER_NON_HEAP), byType(SEMCONV_MEMORY_LIMIT, NON_HEAP)),
                arguments("JVM Total", "C",
                        byInstance(MICROMETER_MEMORY_MAX), byInstance(SEMCONV_MEMORY_LIMIT)),
                arguments("JVM Heap", "A",
                        byArea(MICROMETER_MEMORY_USED, MICROMETER_HEAP), byType(SEMCONV_MEMORY_USED, HEAP)),
                arguments("JVM Heap", "B",
                        byArea(MICROMETER_MEMORY_COMMITTED, MICROMETER_HEAP), byType(SEMCONV_MEMORY_COMMITTED, HEAP)),
                arguments("JVM Heap", "C",
                        byArea(MICROMETER_MEMORY_MAX, MICROMETER_HEAP), byType(SEMCONV_MEMORY_LIMIT, HEAP)),
                arguments("JVM Non-Heap", "A",
                        byArea(MICROMETER_MEMORY_USED, MICROMETER_NON_HEAP), byType(SEMCONV_MEMORY_USED, NON_HEAP)),
                arguments("JVM Non-Heap", "B",
                        byArea(MICROMETER_MEMORY_COMMITTED, MICROMETER_NON_HEAP),
                        byType(SEMCONV_MEMORY_COMMITTED, NON_HEAP)),
                arguments("JVM Non-Heap", "C",
                        byArea(MICROMETER_MEMORY_MAX, MICROMETER_NON_HEAP), byType(SEMCONV_MEMORY_LIMIT, NON_HEAP)),
                arguments("$" + HEAP_POOL_VARIABLE, "A",
                        byPool(MICROMETER_MEMORY_USED, MICROMETER_POOL, HEAP_POOL_VARIABLE),
                        byPool(SEMCONV_MEMORY_USED, SEMCONV_POOL, HEAP_POOL_VARIABLE)),
                arguments("$" + HEAP_POOL_VARIABLE, "B",
                        byPool(MICROMETER_MEMORY_COMMITTED, MICROMETER_POOL, HEAP_POOL_VARIABLE),
                        byPool(SEMCONV_MEMORY_COMMITTED, SEMCONV_POOL, HEAP_POOL_VARIABLE)),
                arguments("$" + HEAP_POOL_VARIABLE, "C",
                        byPool(MICROMETER_MEMORY_MAX, MICROMETER_POOL, HEAP_POOL_VARIABLE),
                        byPool(SEMCONV_MEMORY_LIMIT, SEMCONV_POOL, HEAP_POOL_VARIABLE)),
                arguments("$" + NON_HEAP_POOL_VARIABLE, "A",
                        byPool(MICROMETER_MEMORY_USED, MICROMETER_POOL, NON_HEAP_POOL_VARIABLE),
                        byPool(SEMCONV_MEMORY_USED, SEMCONV_POOL, NON_HEAP_POOL_VARIABLE)),
                arguments("$" + NON_HEAP_POOL_VARIABLE, "B",
                        byPool(MICROMETER_MEMORY_COMMITTED, MICROMETER_POOL, NON_HEAP_POOL_VARIABLE),
                        byPool(SEMCONV_MEMORY_COMMITTED, SEMCONV_POOL, NON_HEAP_POOL_VARIABLE)),
                arguments("$" + NON_HEAP_POOL_VARIABLE, "C",
                        byPool(MICROMETER_MEMORY_MAX, MICROMETER_POOL, NON_HEAP_POOL_VARIABLE),
                        byPool(SEMCONV_MEMORY_LIMIT, SEMCONV_POOL, NON_HEAP_POOL_VARIABLE)));
    }

    @ParameterizedTest(name = "panel \"{0}\" query {1} selects {2} or {3}")
    @MethodSource
    public void openTelemetryDashboardMemoryQueries(String panel, String refId, String micrometerSelector,
            String semconvSelector) {
        assertThat(OPENTELEMETRY_DASHBOARD.panelQuery(panel, refId))
                .contains(micrometerSelector + " or " + semconvSelector);
    }

    static Stream<Arguments> openTelemetryDashboardPoolVariables() {
        return Stream.of(
                arguments(HEAP_POOL_VARIABLE,
                        byArea(MICROMETER_MEMORY_USED, MICROMETER_HEAP), byType(SEMCONV_MEMORY_USED, HEAP)),
                arguments(NON_HEAP_POOL_VARIABLE,
                        byArea(MICROMETER_MEMORY_USED, MICROMETER_NON_HEAP), byType(SEMCONV_MEMORY_USED, NON_HEAP)));
    }

    @ParameterizedTest(name = "variable {0} lists the series of {1} or {2}")
    @MethodSource
    public void openTelemetryDashboardPoolVariables(String variable, String micrometerSelector, String semconvSelector) {
        assertThat(OPENTELEMETRY_DASHBOARD.variableQuery(variable))
                .isEqualTo("query_result(" + micrometerSelector + " or " + semconvSelector + ")");
    }

    static Stream<Arguments> openTelemetryDashboardPoolVariablesExtractPoolNames() {
        return Stream.of(
                arguments(HEAP_POOL_VARIABLE, "G1 Eden Space",
                        queryResultLine(MICROMETER_MEMORY_USED,
                                Map.of(MICROMETER_AREA, MICROMETER_HEAP, MICROMETER_POOL, "G1 Eden Space")),
                        queryResultLine(SEMCONV_MEMORY_USED,
                                Map.of(SEMCONV_MEMORY_TYPE, HEAP, SEMCONV_POOL, "G1 Eden Space"))),
                arguments(NON_HEAP_POOL_VARIABLE, "CodeHeap 'non-nmethods'",
                        queryResultLine(MICROMETER_MEMORY_USED,
                                Map.of(MICROMETER_AREA, MICROMETER_NON_HEAP, MICROMETER_POOL, "CodeHeap 'non-nmethods'")),
                        queryResultLine(SEMCONV_MEMORY_USED,
                                Map.of(SEMCONV_MEMORY_TYPE, NON_HEAP, SEMCONV_POOL, "CodeHeap 'non-nmethods'"))));
    }

    @ParameterizedTest(name = "variable {0} extracts \"{1}\"")
    @MethodSource
    public void openTelemetryDashboardPoolVariablesExtractPoolNames(String variable, String poolName,
            String micrometerSeries, String semconvSeries) {
        Pattern regex = OPENTELEMETRY_DASHBOARD.variableRegex(variable);

        assertThat(extractPoolName(regex, micrometerSeries)).as(micrometerSeries).isEqualTo(poolName);
        assertThat(extractPoolName(regex, semconvSeries)).as(semconvSeries).isEqualTo(poolName);
    }

    @Test
    public void otlpDashboardHeapPoolVariableListsPoolIds() {
        assertThat(OTLP_DASHBOARD.variableQuery(HEAP_POOL_VARIABLE))
                .isEqualTo("label_values(jvm_memory_used_bytes{area=\"heap\"},id)");
    }

    @Test
    public void otlpDashboardHeapPoolPanelSelectsThePool() {
        assertThat(OTLP_DASHBOARD.panelQuery("$" + HEAP_POOL_VARIABLE, "A"))
                .isEqualTo("jvm_memory_used_bytes{id=~\"$jvm_memory_pool_heap\"}");
    }

    private static String byArea(String metric, String area) {
        return metric + "{" + MICROMETER_AREA + "=\"" + area + "\",job=\"$instance\"}";
    }

    private static String byType(String metric, String type) {
        return metric + "{" + SEMCONV_MEMORY_TYPE + "=\"" + type + "\",job=\"$instance\"}";
    }

    private static String byPool(String metric, String poolLabel, String poolVariable) {
        return metric + "{" + poolLabel + "=~\"$" + poolVariable + "\",job=\"$instance\"}";
    }

    private static String byInstance(String metric) {
        return metric + "{job=\"$instance\"}";
    }

    /**
     * Name given by the OTLP to Prometheus translation in the LGTM collector, for example {@code jvm.memory.used}
     * in {@code By} becomes {@code jvm_memory_used_bytes}.
     */
    private static String prometheusMetric(String name, String unit) {
        if (!"By".equals(unit)) {
            throw new IllegalArgumentException("Unsupported unit " + unit);
        }
        return name.replace('.', '_') + "_bytes";
    }

    private static String prometheusLabel(AttributeKey<String> attribute) {
        return attribute.getKey().replace('.', '_');
    }

    /**
     * A series as the {@code query_result()} variable query returns it to the variable regex, with the labels sorted
     * by name like Prometheus does.
     */
    private static String queryResultLine(String metric, Map<String, String> labels) {
        StringJoiner line = new StringJoiner(", ", metric + "{", "} 1.2E7 1700000000000");
        Map<String, String> allLabels = new TreeMap<>(labels);
        allLabels.put("job", "my-app");
        allLabels.forEach((name, value) -> line.add(name + "=\"" + value + "\""));
        return line.toString();
    }

    private static String extractPoolName(Pattern regex, String series) {
        Matcher matcher = regex.matcher(series);
        return matcher.find() ? matcher.group(1) : null;
    }

    @SuppressWarnings("unchecked")
    private record Dashboard(String name, Map<String, Object> json) {

        static Dashboard load(String name) {
            try (InputStream stream = GrafanaDashboardsTest.class.getResourceAsStream("/" + name)) {
                assertThat(stream).as("dashboard %s on the classpath", name).isNotNull();
                return new Dashboard(name, new Yaml().load(stream));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        String panelQuery(String title, String refId) {
            List<Map<String, Object>> panels = new ArrayList<>();
            collectPanels(list(json.get("panels")), panels);
            List<Map<String, Object>> matching = panels.stream()
                    .filter(panel -> title.equals(panel.get("title")))
                    .toList();
            assertThat(matching).as("panels titled \"%s\" in %s", title, name).hasSize(1);
            Map<String, Object> target = list(matching.get(0).get("targets")).stream()
                    .filter(candidate -> refId.equals(candidate.get("refId")))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "No query " + refId + " in panel \"" + title + "\" of " + name));
            return normalize((String) target.get("expr"));
        }

        String variableQuery(String variableName) {
            Object query = variable(variableName).get("query");
            return normalize((String) (query instanceof Map<?, ?> map ? map.get("query") : query));
        }

        Pattern variableRegex(String variableName) {
            String regex = (String) variable(variableName).get("regex");
            assertThat(regex).as("regex of variable %s in %s", variableName, name).startsWith("/").endsWith("/");
            return Pattern.compile(regex.substring(1, regex.length() - 1));
        }

        private Map<String, Object> variable(String variableName) {
            Map<String, Object> templating = (Map<String, Object>) json.get("templating");
            return list(templating.get("list")).stream()
                    .filter(variable -> variableName.equals(variable.get("name")))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No variable " + variableName + " in " + name));
        }

        private static void collectPanels(List<Map<String, Object>> source, List<Map<String, Object>> collected) {
            for (Map<String, Object> panel : source) {
                collected.add(panel);
                collectPanels(list(panel.get("panels")), collected);
            }
        }

        /**
         * Removes the optional whitespace of PromQL selectors, so {@code m{ area = "heap" }} and {@code m{area="heap"}}
         * compare equal, and keeps a single space between the other tokens.
         */
        private static String normalize(String promql) {
            return promql.replaceAll("\\s*(=~|=|,)\\s*", "$1")
                    .replaceAll("\\{\\s+", "{")
                    .replaceAll("\\s+}", "}")
                    .replaceAll("\\s+", " ")
                    .trim();
        }

        private static List<Map<String, Object>> list(Object value) {
            return value == null ? List.of() : (List<Map<String, Object>>) value;
        }
    }
}
