package io.quarkus.observability.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.observability.test.utils.GrafanaClient;
import io.quarkus.observability.test.utils.QueryResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Checks that the JVM memory panels of the "Quarkus Micrometer OpenTelemetry" dashboard show the JVM metrics sent by
 * the application.
 * <p>
 * The dashboard is read from Grafana, and each panel query is sent to Prometheus after its variables are replaced the
 * way Grafana replaces them.
 */
public abstract class LgtmJvmMemoryDashboardTestBase {

    protected static final String MICROMETER_OPENTELEMETRY_DASHBOARD = "edy6473ay1vk0a";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern QUERY_RESULT = Pattern.compile("query_result\\((.+)\\)");
    private static final Pattern LABEL_VALUES = Pattern.compile("label_values\\((.+),\\s*(\\w+)\\)");
    private static final Pattern VARIABLE = Pattern.compile("\\$(\\w+)");

    @ConfigProperty(name = "grafana.endpoint")
    String grafanaEndpoint;

    @ConfigProperty(name = "prometheus.endpoint")
    String prometheusEndpoint;

    protected GrafanaClient client;

    @BeforeEach
    public void waitForJvmMemoryMetrics() {
        client = new GrafanaClient(grafanaEndpoint, prometheusEndpoint, null, "admin", "admin");
        Awaitility.given().ignoreException(UncheckedIOException.class)
                .pollInterval(5, TimeUnit.SECONDS)
                .await().atMost(90, TimeUnit.SECONDS)
                .until(() -> client.query("jvm_memory_used_bytes"), result -> !result.data.result.isEmpty());
    }

    @Test
    public void memoryPoolVariablesListHeapAndNonHeapPools() {
        Dashboard dashboard = dashboard(MICROMETER_OPENTELEMETRY_DASHBOARD);

        assertThat(dashboard.variableValues("jvm_memory_pool_heap")).isNotEmpty().doesNotContain("Metaspace");
        assertThat(dashboard.variableValues("jvm_memory_pool_nonheap")).contains("Metaspace");
    }

    @Test
    public void memoryPanelsShowUsedCommittedAndMax() {
        Dashboard dashboard = dashboard(MICROMETER_OPENTELEMETRY_DASHBOARD);

        for (String title : List.of("Heap used", "Non-Heap used", "JVM Total", "JVM Heap", "JVM Non-Heap")) {
            for (JsonNode target : dashboard.panelTitled(title).path("targets")) {
                assertThat(dashboard.query(target.path("expr").asString(), Map.of()))
                        .as("panel '%s', line '%s'", title, target.path("legendFormat").asString())
                        .isNotEmpty();
            }
        }
    }

    @Test
    public void memoryPoolPanelsShowUsedAndCommittedOfEachPool() {
        assertPoolPanelsShowUsedAndCommittedOfEachPool(dashboard(MICROMETER_OPENTELEMETRY_DASHBOARD));
    }

    /**
     * Grafana repeats the memory pool panels once per pool listed by their variable. Pools without a maximum size, like
     * the G1 Eden Space, have no "max" line, so only the "used" and "committed" lines are expected for every pool.
     */
    protected void assertPoolPanelsShowUsedAndCommittedOfEachPool(Dashboard dashboard) {
        for (String variable : List.of("jvm_memory_pool_heap", "jvm_memory_pool_nonheap")) {
            JsonNode panel = dashboard.panelRepeatedBy(variable);
            Set<String> pools = dashboard.variableValues(variable);
            assertThat(pools).as("values of variable '%s'", variable).isNotEmpty();
            for (String pool : pools) {
                for (JsonNode target : panel.path("targets")) {
                    String line = target.path("legendFormat").asString();
                    if (Set.of("used", "committed").contains(line)) {
                        assertThat(dashboard.query(target.path("expr").asString(), Map.of(variable, pool)))
                                .as("pool '%s', line '%s'", pool, line)
                                .isNotEmpty();
                    }
                }
            }
        }
    }

    protected Dashboard dashboard(String uid) {
        return new Dashboard(MAPPER.readTree(client.dashboard(uid)).path("dashboard"));
    }

    protected class Dashboard {

        private final JsonNode json;

        Dashboard(JsonNode json) {
            this.json = json;
        }

        JsonNode panelTitled(String title) {
            return panels().filter(panel -> title.equals(panel.path("title").asString()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No panel titled '" + title + "'"));
        }

        JsonNode panelRepeatedBy(String variable) {
            return panels().filter(panel -> variable.equals(panel.path("repeat").asString()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No panel repeated by variable '" + variable + "'"));
        }

        private Stream<JsonNode> panels() {
            return json.path("panels").values().stream()
                    .flatMap(panel -> Stream.concat(Stream.of(panel), panel.path("panels").values().stream()));
        }

        /**
         * Lists the values Grafana offers for a variable, for the two kinds of query used by the dashboards:
         * {@code label_values(selector, label)} and {@code query_result(query)} followed by the variable regex.
         */
        Set<String> variableValues(String name) {
            JsonNode variable = variable(name);
            JsonNode query = variable.path("query");
            String definition = query.isObject() ? query.path("query").asString() : query.asString();

            Matcher labelValues = LABEL_VALUES.matcher(definition);
            if (labelValues.matches()) {
                return query(labelValues.group(1), Map.of()).stream()
                        .map(series -> labels(series).get(labelValues.group(2)))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(TreeSet::new));
            }

            Matcher queryResult = QUERY_RESULT.matcher(definition);
            assertThat(queryResult.matches()).as("query of variable '%s': %s", name, definition).isTrue();
            String regex = variable.path("regex").asString();
            Pattern pattern = Pattern.compile(regex.substring(1, regex.length() - 1));
            Set<String> values = new TreeSet<>();
            for (QueryResult.ResultItem series : query(queryResult.group(1), Map.of())) {
                Matcher matcher = pattern.matcher(queryResultLine(series));
                if (matcher.find()) {
                    values.add(matcher.group(1));
                }
            }
            return values;
        }

        /**
         * Sends a panel query to Prometheus, using the first value of the {@code instance} variable, as Grafana does
         * by default, and the given values for the other variables.
         */
        List<QueryResult.ResultItem> query(String expr, Map<String, String> variables) {
            Matcher matcher = VARIABLE.matcher(expr);
            StringBuilder resolved = new StringBuilder();
            while (matcher.find()) {
                String name = matcher.group(1);
                String value = "instance".equals(name) ? instance() : escapeRegex(variables.get(name));
                matcher.appendReplacement(resolved, Matcher.quoteReplacement(value));
            }
            matcher.appendTail(resolved);
            return client.query(resolved.toString()).data.result;
        }

        private String instance() {
            return variableValues("instance").stream()
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Variable 'instance' has no value"));
        }

        private JsonNode variable(String name) {
            return json.path("templating").path("list").values().stream()
                    .filter(variable -> name.equals(variable.path("name").asString()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No variable named '" + name + "'"));
        }
    }

    /**
     * Formats a series the way Grafana shows it to the regex of a {@code query_result} variable, for example
     * {@code jvm_memory_used_bytes{id="G1 Eden Space", job="my-app"} 1.2E7 1700000000}.
     */
    private static String queryResultLine(QueryResult.ResultItem series) {
        Map<String, String> labels = labels(series);
        String name = labels.remove("__name__");
        return labels.entrySet().stream()
                .map(label -> label.getKey() + "=\"" + label.getValue() + "\"")
                .collect(Collectors.joining(", ", name + "{", "} " + series.value.get(1) + " " + series.value.get(0)));
    }

    private static Map<String, String> labels(QueryResult.ResultItem series) {
        Map<String, String> labels = new TreeMap<>(series.metric.labels);
        labels.put("__name__", series.metric.name);
        if (series.metric.job != null) {
            labels.put("job", series.metric.job);
        }
        if (series.metric.test != null) {
            labels.put("test", series.metric.test);
        }
        return labels;
    }

    /**
     * Grafana escapes the regex characters of a single variable value used in a Prometheus label matcher.
     */
    private static String escapeRegex(String value) {
        if (value == null) {
            throw new AssertionError("Missing variable value");
        }
        return value.replaceAll("[\\\\^$.|?*+()\\[\\]{}]", "\\\\\\\\$0");
    }
}
