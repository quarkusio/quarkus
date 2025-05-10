package io.quarkus.kafka.streams.runtime.dev.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

public class KafkaStreamsJsonRPCServiceTest {
    @InjectMocks
    KafkaStreamsJsonRPCService rpcService = new KafkaStreamsJsonRPCService();

    @Test
    public void shouldParsingStayConstant() {
        final var expectedDescribe = "Topologies:\n"
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
        final var actual = rpcService.parseTopologyDescription(expectedDescribe);

        assertEquals(expectedDescribe, actual.getString("describe"));
        assertEquals("[0, 1, 2]", actual.getString("subTopologies"));
        assertEquals("[notification\\..+, temperature.values, weather-stations]", actual.getString("sources"));
        assertEquals("[temperatures-aggregated]", actual.getString("sinks"));
        assertEquals("[weather-stations-STATE-STORE-0000000000, weather-stations-store]", actual.getString("stores"));
        assertEquals("digraph {\n"
                + " fontname=Helvetica; fontsize=10;\n"
                + " node [style=filled fillcolor=white color=\"#C9B7DD\" shape=box fontname=Helvetica fontsize=10];\n"
                + " \"weather-stations\" [label=\"weather\\nstations\" shape=invhouse margin=\"0,0\"];\n"
                + " \"KSTREAM-SOURCE-0000000001\" [label=\"KSTREAM\\nSOURCE\\n0000000001\"];\n"
                + " \"KTABLE-SOURCE-0000000002\" [label=\"KTABLE\\nSOURCE\\n0000000002\"];\n"
                + " \"weather-stations-STATE-STORE-0000000000\" [label=\"weather\\nstations\\nSTATE\\nSTORE\\n0000000000\" shape=cylinder];\n"
                + " \"temperature.values\" [label=\"temperature.values\" shape=invhouse margin=\"0,0\"];\n"
                + " \"KSTREAM-SOURCE-0000000003\" [label=\"KSTREAM\\nSOURCE\\n0000000003\"];\n"
                + " \"KSTREAM-LEFTJOIN-0000000004\" [label=\"KSTREAM\\nLEFTJOIN\\n0000000004\"];\n"
                + " \"KSTREAM-AGGREGATE-0000000005\" [label=\"KSTREAM\\nAGGREGATE\\n0000000005\"];\n"
                + " \"weather-stations-store\" [label=\"weather\\nstations\\nstore\" shape=cylinder];\n"
                + " \"KTABLE-TOSTREAM-0000000006\" [label=\"KTABLE\\nTOSTREAM\\n0000000006\"];\n"
                + " \"KSTREAM-SINK-0000000007\" [label=\"KSTREAM\\nSINK\\n0000000007\"];\n"
                + " \"temperatures-aggregated\" [label=\"temperatures\\naggregated\" shape=house margin=\"0,0\"];\n"
                + " REGEX_12 [label=\"notification\\\\..+\" shape=invhouse style=dashed margin=\"0,0\"];\n"
                + " \"KSTREAM-SOURCE-0000000008\" [label=\"KSTREAM\\nSOURCE\\n0000000008\"];\n"
                + " \"KSTREAM-FOREACH-0000000009\" [label=\"KSTREAM\\nFOREACH\\n0000000009\"];\n"
                + " subgraph cluster0 {\n"
                + "  label=\"Sub-Topology: 0\"; color=\"#C8C879\"; bgcolor=\"#FFFFDE\";\n"
                + "  \"KSTREAM-SOURCE-0000000001\";\n"
                + "  \"KTABLE-SOURCE-0000000002\";\n"
                + " }\n"
                + " subgraph cluster1 {\n"
                + "  label=\"Sub-Topology: 1\"; color=\"#C8C879\"; bgcolor=\"#FFFFDE\";\n"
                + "  \"KSTREAM-SOURCE-0000000003\";\n"
                + "  \"KSTREAM-LEFTJOIN-0000000004\";\n"
                + "  \"KSTREAM-AGGREGATE-0000000005\";\n"
                + "  \"KTABLE-TOSTREAM-0000000006\";\n"
                + "  \"KSTREAM-SINK-0000000007\";\n"
                + " }\n"
                + " subgraph cluster2 {\n"
                + "  label=\"Sub-Topology: 2\"; color=\"#C8C879\"; bgcolor=\"#FFFFDE\";\n"
                + "  \"KSTREAM-SOURCE-0000000008\";\n"
                + "  \"KSTREAM-FOREACH-0000000009\";\n"
                + " }\n"
                + " \"weather-stations\" -> \"KSTREAM-SOURCE-0000000001\";\n"
                + " \"KSTREAM-SOURCE-0000000001\" -> \"KTABLE-SOURCE-0000000002\";\n"
                + " \"KTABLE-SOURCE-0000000002\" -> \"weather-stations-STATE-STORE-0000000000\";\n"
                + " \"temperature.values\" -> \"KSTREAM-SOURCE-0000000003\";\n"
                + " \"KSTREAM-SOURCE-0000000003\" -> \"KSTREAM-LEFTJOIN-0000000004\";\n"
                + " \"KSTREAM-LEFTJOIN-0000000004\" -> \"KSTREAM-AGGREGATE-0000000005\";\n"
                + " \"KSTREAM-AGGREGATE-0000000005\" -> \"weather-stations-store\";\n"
                + " \"KSTREAM-AGGREGATE-0000000005\" -> \"KTABLE-TOSTREAM-0000000006\";\n"
                + " \"KTABLE-TOSTREAM-0000000006\" -> \"KSTREAM-SINK-0000000007\";\n"
                + " \"KSTREAM-SINK-0000000007\" -> \"temperatures-aggregated\";\n"
                + " REGEX_12 -> \"KSTREAM-SOURCE-0000000008\";\n"
                + " \"KSTREAM-SOURCE-0000000008\" -> \"KSTREAM-FOREACH-0000000009\";\n"
                + "}", actual.getString("graphviz"));
        assertEquals("graph TD\n"
                + " weather-stations[weather-stations] --> KSTREAM-SOURCE-0000000001(KSTREAM-<br>SOURCE-<br>0000000001)\n"
                + " KTABLE-SOURCE-0000000002[KTABLE-<br>SOURCE-<br>0000000002] --> weather-stations-STATE-STORE-0000000000(weather-<br>stations-<br>STATE-<br>STORE-<br>0000000000)\n"
                + " temperature.values[temperature.values] --> KSTREAM-SOURCE-0000000003(KSTREAM-<br>SOURCE-<br>0000000003)\n"
                + " KSTREAM-AGGREGATE-0000000005[KSTREAM-<br>AGGREGATE-<br>0000000005] --> weather-stations-store(weather-<br>stations-<br>store)\n"
                + " KSTREAM-SINK-0000000007[KSTREAM-<br>SINK-<br>0000000007] --> temperatures-aggregated(temperatures-aggregated)\n"
                + " REGEX_5[notification\\..+] --> KSTREAM-SOURCE-0000000008(KSTREAM-<br>SOURCE-<br>0000000008)\n"
                + " subgraph Sub-Topology: 0\n"
                + "  KSTREAM-SOURCE-0000000001[KSTREAM-<br>SOURCE-<br>0000000001] --> KTABLE-SOURCE-0000000002(KTABLE-<br>SOURCE-<br>0000000002)\n"
                + " end\n"
                + " subgraph Sub-Topology: 1\n"
                + "  KSTREAM-SOURCE-0000000003[KSTREAM-<br>SOURCE-<br>0000000003] --> KSTREAM-LEFTJOIN-0000000004(KSTREAM-<br>LEFTJOIN-<br>0000000004)\n"
                + "  KSTREAM-LEFTJOIN-0000000004[KSTREAM-<br>LEFTJOIN-<br>0000000004] --> KSTREAM-AGGREGATE-0000000005(KSTREAM-<br>AGGREGATE-<br>0000000005)\n"
                + "  KSTREAM-AGGREGATE-0000000005[KSTREAM-<br>AGGREGATE-<br>0000000005] --> KTABLE-TOSTREAM-0000000006(KTABLE-<br>TOSTREAM-<br>0000000006)\n"
                + "  KTABLE-TOSTREAM-0000000006[KTABLE-<br>TOSTREAM-<br>0000000006] --> KSTREAM-SINK-0000000007(KSTREAM-<br>SINK-<br>0000000007)\n"
                + " end\n"
                + " subgraph Sub-Topology: 2\n"
                + "  KSTREAM-SOURCE-0000000008[KSTREAM-<br>SOURCE-<br>0000000008] --> KSTREAM-FOREACH-0000000009(KSTREAM-<br>FOREACH-<br>0000000009)\n"
                + " end", actual.getString("mermaid"));
    }
}
