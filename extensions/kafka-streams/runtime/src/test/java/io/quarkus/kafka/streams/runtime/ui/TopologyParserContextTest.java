package io.quarkus.kafka.streams.runtime.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Verifies the structured node/edge view the Prod UI graph renderer consumes.
 * The same topology description feeds the Graphviz/Mermaid renderings, so the
 * structured data must describe the very same nodes and directed edges.
 */
public class TopologyParserContextTest {

    private static final String DESCRIBE = "Topologies:\n"
            + "Sub-topology: 0 for global store (will not generate tasks)\n"
            + "  Source: KSTREAM-SOURCE-0000000001 (topics: [weather-stations])\n"
            + "    --> KTABLE-SOURCE-0000000002\n"
            + "  Processor: KTABLE-SOURCE-0000000002 (stores: [weather-stations-STATE-STORE-0000000000])\n"
            + "    --> none\n"
            + "    <-- KSTREAM-SOURCE-0000000001\n"
            + "Sub-topology: 1\n"
            + "  Source: KSTREAM-SOURCE-0000000003 (topics: [temperature.values])\n"
            + "    --> KSTREAM-LEFTJOIN-0000000004\n"
            + "  Processor: KSTREAM-LEFTJOIN-0000000004 (stores: [])\n"
            + "    --> KSTREAM-AGGREGATE-0000000005\n"
            + "    <-- KSTREAM-SOURCE-0000000003\n"
            + "  Processor: KSTREAM-AGGREGATE-0000000005 (stores: [weather-stations-store])\n"
            + "    --> KTABLE-TOSTREAM-0000000006\n"
            + "    <-- KSTREAM-LEFTJOIN-0000000004\n"
            + "  Processor: KTABLE-TOSTREAM-0000000006 (stores: [])\n"
            + "    --> KSTREAM-SINK-0000000007\n"
            + "    <-- KSTREAM-AGGREGATE-0000000005\n"
            + "  Sink: KSTREAM-SINK-0000000007 (topic: temperatures-aggregated)\n"
            + "    <-- KTABLE-TOSTREAM-0000000006\n"
            + "\n"
            + "  Sub-topology: 2\n"
            + "    Source: KSTREAM-SOURCE-0000000008 (topics: notification\\..+)\n"
            + "      --> KSTREAM-FOREACH-0000000009\n"
            + "    Processor: KSTREAM-FOREACH-0000000009 (stores: [])\n"
            + "      --> none\n"
            + "      <-- KSTREAM-SOURCE-0000000008";

    @Test
    public void exposesTypedNodes() {
        TopologyParserContext context = TopologyDescriptionParser.parse(DESCRIBE);
        List<Map<String, String>> nodes = context.getNodes();

        // The structured view must classify each node by its role.
        assertThat(nodes)
                .extracting(n -> n.get("id"), n -> n.get("type"))
                .contains(
                        tuple("weather-stations", "source"),
                        tuple("temperature.values", "source"),
                        tuple("temperatures-aggregated", "sink"),
                        tuple("weather-stations-STATE-STORE-0000000000", "store"),
                        tuple("weather-stations-store", "store"),
                        tuple("KSTREAM-AGGREGATE-0000000005", "processor"));

        // A regex source keeps its readable pattern as the label, distinct from its generated id.
        assertThat(nodes)
                .filteredOn(n -> "source".equals(n.get("type")) && n.get("id").startsWith("REGEX_"))
                .singleElement()
                .satisfies(n -> assertThat(n.get("label")).isEqualTo("notification\\..+"));

        // Ids are unique - a processor is never emitted twice even though it appears in many edges.
        assertThat(nodes).extracting(n -> n.get("id")).doesNotHaveDuplicates();
    }

    @Test
    public void exposesDirectedEdges() {
        TopologyParserContext context = TopologyDescriptionParser.parse(DESCRIBE);
        List<Map<String, String>> edges = context.getEdges();

        assertThat(edges)
                .extracting(e -> e.get("source"), e -> e.get("target"))
                .contains(
                        tuple("weather-stations", "KSTREAM-SOURCE-0000000001"),
                        tuple("KTABLE-SOURCE-0000000002", "weather-stations-STATE-STORE-0000000000"),
                        tuple("KSTREAM-SINK-0000000007", "temperatures-aggregated"));

        // Every edge endpoint must resolve to a declared node, so the client can draw the graph.
        List<String> ids = context.getNodes().stream().map(n -> n.get("id")).toList();
        assertThat(edges).allSatisfy(e -> {
            assertThat(ids).contains(e.get("source"));
            assertThat(ids).contains(e.get("target"));
        });
    }
}
