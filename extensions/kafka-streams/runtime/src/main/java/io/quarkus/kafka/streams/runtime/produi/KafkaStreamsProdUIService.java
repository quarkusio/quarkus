package io.quarkus.kafka.streams.runtime.produi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.apache.kafka.streams.Topology;

import io.quarkus.kafka.streams.runtime.ui.TopologyDescriptionParser;
import io.quarkus.kafka.streams.runtime.ui.TopologyParserContext;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.smallrye.common.annotation.NonBlocking;

/**
 * Read-only Prod UI view of the Kafka Streams topology. It exposes only the
 * topology description and its derived structure (sub-topologies, sources,
 * sinks, stores, Graphviz and Mermaid renderings) - there is nothing to mutate
 * and no secrets are involved. It lives in the always-present runtime module
 * (the Dev UI service lives in the dev-only module) and returns plain maps so no
 * JSON library is needed on the runtime classpath.
 */
@ApplicationScoped
public class KafkaStreamsProdUIService {

    @Inject
    Instance<Topology> topologyProvider;

    @NonBlocking
    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only view of the Kafka Streams topology")
    public Map<String, Object> getTopology() {
        var topologyDescription = "";
        if (topologyProvider.isResolvable()) {
            final var describe = topologyProvider.get().describe();
            topologyDescription = describe != null ? describe.toString() : "";
        }

        TopologyParserContext context = TopologyDescriptionParser.parse(topologyDescription);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("describe", topologyDescription);
        res.put("subTopologies", new ArrayList<>(context.getSubTopologies()));
        res.put("sources", new ArrayList<>(context.getSources()));
        res.put("sinks", new ArrayList<>(context.getSinks()));
        res.put("stores", new ArrayList<>(context.getStores()));
        res.put("nodes", context.getNodes());
        res.put("edges", context.getEdges());
        res.put("graphviz", context.getGraphviz());
        res.put("mermaid", context.getMermaid());
        return res;
    }
}
