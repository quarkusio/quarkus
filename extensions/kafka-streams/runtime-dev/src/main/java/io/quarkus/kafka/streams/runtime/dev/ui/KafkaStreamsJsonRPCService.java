package io.quarkus.kafka.streams.runtime.dev.ui;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.apache.kafka.streams.Topology;

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
        final var res = new JsonObject();

        final var context = new TopologyParserContext();
        Arrays.stream(topologyDescription.split("\n"))
                .map(String::trim)
                .forEachOrdered(line -> Stream.of(SUB_TOPOLOGY, SOURCE, PROCESSOR, SINK, RIGHT_ARROW)
                        .filter(itemParser -> itemParser.test(line))
                        .forEachOrdered(itemParser -> itemParser.accept(context)));

        res
                .put("describe", topologyDescription)
                .put("subTopologies", context.subTopologies)
                .put("sources", context.sources)
                .put("sinks", context.sinks)
                .put("stores", context.stores)
                .put("graphviz", context.graphviz.toGraph())
                .put("mermaid", context.mermaid.toGraph());

        return res;
    }

    private interface RawTopologyItemParser extends Predicate<String>, Consumer<TopologyParserContext> {
    }

    private static final RawTopologyItemParser SUB_TOPOLOGY = new RawTopologyItemParser() {
        private final Pattern subTopologyPattern = Pattern.compile("Sub-topology: (?<subTopology>[0-9]*).*");
        private Matcher matcher;

        @Override
        public boolean test(String line) {
            matcher = subTopologyPattern.matcher(line);
            return matcher.matches();
        }

        @Override
        public void accept(TopologyParserContext context) {
            context.addSubTopology(matcher.group("subTopology"));
        }
    };

    private static final RawTopologyItemParser SOURCE = new RawTopologyItemParser() {
        private final Pattern sourcePattern = Pattern
                .compile("Source:\\s+(?<source>\\S+)\\s+\\(topics:\\s+((\\[(?<topics>.*)\\])|(?<regex>.*)\\)).*");
        private Matcher matcher;

        @Override
        public boolean test(String line) {
            matcher = sourcePattern.matcher(line);
            return matcher.matches();
        }

        @Override
        public void accept(TopologyParserContext context) {
            if (matcher.group("topics") != null) {
                context.addSources(matcher.group("source"), matcher.group("topics").split(","));
            } else if (matcher.group("regex") != null) {
                context.addRegexSource(matcher.group("source"), matcher.group("regex"));
            }
        }
    };

    private static final RawTopologyItemParser PROCESSOR = new RawTopologyItemParser() {
        private final Pattern processorPattern = Pattern
                .compile("Processor:\\s+(?<processor>\\S+)\\s+\\(stores:\\s+\\[(?<stores>.*)\\]\\).*");
        private Matcher matcher;
        private String line;

        @Override
        public boolean test(String line) {
            this.line = line;
            matcher = processorPattern.matcher(line);
            return matcher.matches();
        }

        @Override
        public void accept(TopologyParserContext context) {
            context.addStores(matcher.group("stores").split(","), matcher.group("processor"), line.contains("JOIN"));
        }
    };

    private static final RawTopologyItemParser SINK = new RawTopologyItemParser() {
        private final Pattern sinkPattern = Pattern.compile("Sink:\\s+(?<sink>\\S+)\\s+\\(topic:\\s+(?<topic>.*)\\).*");
        private Matcher matcher;

        @Override
        public boolean test(String line) {
            matcher = sinkPattern.matcher(line);
            return matcher.matches();
        }

        @Override
        public void accept(TopologyParserContext context) {
            context.addSink(matcher.group("sink"), matcher.group("topic"));
        }
    };

    private static final RawTopologyItemParser RIGHT_ARROW = new RawTopologyItemParser() {
        private final Pattern rightArrowPattern = Pattern.compile("\\s*-->\\s+(?<targets>.*)");
        private Matcher matcher;

        @Override
        public boolean test(String line) {
            matcher = rightArrowPattern.matcher(line);
            return matcher.matches();
        }

        @Override
        public void accept(TopologyParserContext context) {
            context.addTargets(matcher.group("targets").split(","));
        }
    };
}
