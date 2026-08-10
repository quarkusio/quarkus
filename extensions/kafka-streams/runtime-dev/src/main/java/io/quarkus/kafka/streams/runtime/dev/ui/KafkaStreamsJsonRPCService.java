package io.quarkus.kafka.streams.runtime.dev.ui;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.apache.kafka.streams.Topology;

import io.quarkus.kafka.streams.runtime.ui.TopologyDescriptionParser;
import io.smallrye.common.annotation.NonBlocking;
import io.vertx.core.json.JsonObject;

public class KafkaStreamsJsonRPCService {
    @Inject
    Instance<Topology> topologyProvider;

    @NonBlocking
    public JsonObject getTopology() {
        var topologyDescription = "";
        if (topologyProvider.isResolvable()) {
            final var describe = topologyProvider.get().describe();
            topologyDescription = describe != null ? describe.toString() : "";
        }
        return parseTopologyDescription(topologyDescription);
    }

    JsonObject parseTopologyDescription(String topologyDescription) {
        final var context = TopologyDescriptionParser.parse(topologyDescription);

        return new JsonObject()
                .put("describe", topologyDescription)
                .put("subTopologies", context.getSubTopologies())
                .put("sources", context.getSources())
                .put("sinks", context.getSinks())
                .put("stores", context.getStores())
                .put("graphviz", context.getGraphviz())
                .put("mermaid", context.getMermaid());
    }
}
