package io.quarkus.observability.testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

public class GrafanaDashboardsTest {

    private static final String PROMETHEUS_DASHBOARD = "grafana-dashboard-quarkus-micrometer-prometheus.json";
    private static final String OTLP_DASHBOARD = "grafana-dashboard-quarkus-micrometer-otlp.json";
    private static final String OPENTELEMETRY_DASHBOARD = "grafana-dashboard-quarkus-micrometer-opentelemetry.json";

    private static final List<String> MICROMETER_DASHBOARDS = List.of(
            PROMETHEUS_DASHBOARD,
            OTLP_DASHBOARD,
            OPENTELEMETRY_DASHBOARD);

    private static final Map<String, String> POOL_NAME_SAMPLES = Map.of(
            "jvm_memory_used_bytes{area=\"heap\", id=\"G1 Eden Space\", job=\"myservice\"} 1.2E7 1700000000000",
            "G1 Eden Space",
            "jvm_memory_used_bytes{job=\"myservice\", jvm_memory_pool_name=\"G1 Old Gen\", jvm_memory_type=\"heap\", service_instance_id=\"abc\"} 3.4E7 1700000000000",
            "G1 Old Gen",
            "jvm_memory_used_bytes{area=\"nonheap\", id=\"CodeHeap 'non-nmethods'\", job=\"myservice\"} 1.0E6 1700000000000",
            "CodeHeap 'non-nmethods'",
            "jvm_memory_used_bytes{job=\"myservice\", jvm_memory_pool_name=\"Metaspace\", jvm_memory_type=\"non_heap\", service_instance_id=\"abc\"} 5.0E6 1700000000000",
            "Metaspace");

    private static final Map<String, List<String>> REQUIRED_POOL_SELECTORS = Map.of(
            "jvm_memory_pool_heap", List.of("area=\"heap\"", "jvm_memory_type=\"heap\""),
            "jvm_memory_pool_nonheap", List.of("area=\"nonheap\"", "jvm_memory_type=\"non_heap\""));

    private static final Map<String, String> SEMANTIC_CONVENTION_COMPANIONS = Map.of(
            "area=\"heap\"", "jvm_memory_type=\"heap\"",
            "area=\"nonheap\"", "jvm_memory_type=\"non_heap\"",
            "id=~\"$jvm_memory_pool_heap\"", "jvm_memory_pool_name=~\"$jvm_memory_pool_heap\"",
            "id=~\"$jvm_memory_pool_nonheap\"", "jvm_memory_pool_name=~\"$jvm_memory_pool_nonheap\"",
            "jvm_memory_max_bytes", "jvm_memory_limit_bytes");

    @Test
    public void repeatedPanelsQueryTheirRepeatVariable() {
        SoftAssertions softly = new SoftAssertions();
        for (String dashboardName : MICROMETER_DASHBOARDS) {
            for (Map<String, Object> panel : collectPanels(loadDashboard(dashboardName))) {
                String repeat = stringValue(panel.get("repeat"));
                List<Map<String, Object>> targets = mapList(panel.get("targets")).stream()
                        .filter(target -> !stringValue(target.get("expr")).isEmpty())
                        .toList();
                if (repeat.isEmpty() || targets.isEmpty()) {
                    continue;
                }
                for (Map<String, Object> target : targets) {
                    softly.assertThat(stringValue(target.get("expr")))
                            .as("%s, panel '%s', refId %s should query $%s",
                                    dashboardName, panel.get("title"), target.get("refId"), repeat)
                            .contains("$" + repeat);
                }
            }
        }
        softly.assertAll();
    }

    @Test
    public void memoryPoolVariablesExtractPoolNames() {
        SoftAssertions softly = new SoftAssertions();
        for (String dashboardName : MICROMETER_DASHBOARDS) {
            for (Map<String, Object> variable : templatingVariables(loadDashboard(dashboardName))) {
                String name = stringValue(variable.get("name"));
                if (!name.startsWith("jvm_memory_pool_")) {
                    continue;
                }
                softly.assertThat(normalize(variableQuery(variable)))
                        .as("%s, variable %s query should extract a pool name, not the area", dashboardName, name)
                        .doesNotContain(",area)");
                String definition = normalize(stringValue(variable.get("definition")));
                if (!definition.isEmpty()) {
                    softly.assertThat(definition)
                            .as("%s, variable %s definition should extract a pool name, not the area",
                                    dashboardName, name)
                            .doesNotContain(",area)");
                }
            }
        }
        softly.assertAll();
    }

    @Test
    public void openTelemetryDashboardPoolVariablesResolveBothLabelConventions() {
        Map<String, Object> dashboard = loadDashboard(OPENTELEMETRY_DASHBOARD);
        for (String variableName : REQUIRED_POOL_SELECTORS.keySet()) {
            Map<String, Object> variable = findVariable(dashboard, variableName);

            String regex = stringValue(variable.get("regex"));
            assertThat(regex).as("%s regex", variableName).isNotEmpty();
            Pattern pattern = toPattern(regex);
            for (Map.Entry<String, String> sample : POOL_NAME_SAMPLES.entrySet()) {
                assertThat(extractPoolName(pattern, sample.getKey()))
                        .as("%s regex applied to sample line %s", variableName, sample.getKey())
                        .isEqualTo(sample.getValue());
            }

            String query = normalize(variableQuery(variable));
            for (String selector : REQUIRED_POOL_SELECTORS.get(variableName)) {
                assertThat(query)
                        .as("%s query should select pools with %s", variableName, selector)
                        .contains(selector);
            }
        }
    }

    @Test
    public void openTelemetryDashboardMemoryQueriesSupportSemanticConventions() {
        SoftAssertions softly = new SoftAssertions();
        for (Map<String, Object> panel : collectPanels(loadDashboard(OPENTELEMETRY_DASHBOARD))) {
            for (Map<String, Object> target : mapList(panel.get("targets"))) {
                String expr = normalize(stringValue(target.get("expr")));
                String description = "panel '" + panel.get("title") + "' refId " + target.get("refId");
                for (Map.Entry<String, String> companion : SEMANTIC_CONVENTION_COMPANIONS.entrySet()) {
                    if (expr.contains(companion.getKey())) {
                        softly.assertThat(expr)
                                .as("%s uses %s and should also use %s",
                                        description, companion.getKey(), companion.getValue())
                                .contains(companion.getValue());
                    }
                }
            }
        }
        softly.assertAll();
    }

    private static Map<String, Object> loadDashboard(String name) {
        try (InputStream stream = GrafanaDashboardsTest.class.getResourceAsStream("/" + name)) {
            assertThat(stream).as("dashboard %s on classpath", name).isNotNull();
            return new Yaml().load(stream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Map<String, Object>> collectPanels(Map<String, Object> dashboard) {
        List<Map<String, Object>> panels = new ArrayList<>();
        collectPanels(mapList(dashboard.get("panels")), panels);
        return panels;
    }

    private static void collectPanels(List<Map<String, Object>> source, List<Map<String, Object>> out) {
        for (Map<String, Object> panel : source) {
            out.add(panel);
            collectPanels(mapList(panel.get("panels")), out);
        }
    }

    private static List<Map<String, Object>> templatingVariables(Map<String, Object> dashboard) {
        Object templating = dashboard.get("templating");
        return templating instanceof Map<?, ?> map ? mapList(map.get("list")) : List.of();
    }

    private static Map<String, Object> findVariable(Map<String, Object> dashboard, String name) {
        return templatingVariables(dashboard).stream()
                .filter(variable -> name.equals(variable.get("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing templating variable " + name));
    }

    private static String variableQuery(Map<String, Object> variable) {
        Object query = variable.get("query");
        if (query instanceof Map<?, ?> map) {
            return stringValue(map.get("query"));
        }
        return stringValue(query);
    }

    private static Pattern toPattern(String grafanaRegex) {
        assertThat(grafanaRegex).startsWith("/").endsWith("/");
        return Pattern.compile(grafanaRegex.substring(1, grafanaRegex.length() - 1));
    }

    private static String extractPoolName(Pattern pattern, String line) {
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String normalize(String text) {
        return text.replaceAll("\\s+", "");
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> mapList(Object value) {
        return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }
}
