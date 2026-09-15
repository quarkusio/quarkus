package io.quarkus.kafka.streams.runtime.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Holds the result of parsing a Kafka Streams topology description, together
 * with Graphviz and Mermaid renderings of it. It is pure JDK (no JSON library)
 * and read-only, so it can be shared by both the dev-only JSON-RPC service and
 * the always-present Prod UI service.
 */
public final class TopologyParserContext {
    String currentNode = "";
    final Set<String> subTopologies = new TreeSet<>();
    final Set<String> sources = new TreeSet<>();
    final Set<String> sinks = new TreeSet<>();
    final Set<String> stores = new TreeSet<>();
    final Graphviz graphviz = new Graphviz();
    final Mermaid mermaid = new Mermaid();

    public Set<String> getSubTopologies() {
        return subTopologies;
    }

    public Set<String> getSources() {
        return sources;
    }

    public Set<String> getSinks() {
        return sinks;
    }

    public Set<String> getStores() {
        return stores;
    }

    public String getGraphviz() {
        return graphviz.toGraph();
    }

    public String getMermaid() {
        return mermaid.toGraph();
    }

    /**
     * The topology as a structured node list, one entry per node with keys
     * {@code id}, {@code label}, {@code type} (source/sink/store/processor) and
     * {@code subTopology}. This is the same graph the Graphviz/Mermaid renderings
     * describe, exposed as data so a client can draw it (e.g. an ECharts graph)
     * without parsing DOT.
     */
    public List<Map<String, String>> getNodes() {
        return new ArrayList<>(graphviz.structuredNodes.values());
    }

    /**
     * The directed edges of the topology, one entry per edge with keys
     * {@code source} and {@code target} referencing node {@code id}s.
     */
    public List<Map<String, String>> getEdges() {
        return graphviz.structuredEdges;
    }

    void addSubTopology(String subTopology) {
        final var sanitizedSubTopology = sanitize(subTopology);
        subTopologies.add(sanitizedSubTopology);
        graphviz.addSubTopology(sanitizedSubTopology);
        mermaid.addSubTopology(sanitizedSubTopology);
    }

    void addSink(String sink, String topic) {
        final var sanitizedTopic = sanitize(topic);
        sinks.add(sanitizedTopic);
        final var sanitizedSink = sanitize(sink);
        currentNode = sanitize(sanitizedSink);
        graphviz.addSink(sanitizedSink, sanitizedTopic);
        mermaid.addSink(sanitizedSink, sanitizedTopic);
    }

    void addSources(String source, String[] topics) {
        currentNode = sanitize(source);
        Arrays.stream(topics)
                .map(String::trim).filter(topic -> !topic.isEmpty())
                .forEachOrdered(topic -> {
                    final var sanitizedTopic = sanitize(topic);
                    sources.add(sanitizedTopic);
                    graphviz.addSource(currentNode, sanitizedTopic);
                    mermaid.addSource(currentNode, sanitizedTopic);
                });
    }

    void addRegexSource(String source, String regex) {
        currentNode = sanitize(source);
        final var sanitizedRegex = sanitize(regex);
        if (!sanitizedRegex.isEmpty()) {
            sources.add(sanitizedRegex);
            graphviz.addRegexSource(currentNode, sanitizedRegex);
            mermaid.addRegexSource(currentNode, sanitizedRegex);
        }
    }

    void addStores(String[] stores, String processor, boolean join) {
        currentNode = sanitize(processor);
        Arrays.stream(stores)
                .map(String::trim).filter(store -> !store.isEmpty())
                .forEachOrdered(store -> {
                    final var sanitizedStore = sanitize(store);
                    this.stores.add(sanitizedStore);
                    graphviz.addStore(sanitizedStore, currentNode, join);
                    mermaid.addStore(sanitizedStore, currentNode, join);
                });
    }

    void addTargets(String[] targets) {
        Arrays.stream(targets)
                .map(String::trim).filter(target -> !("none".equals(target) || target.isEmpty()))
                .forEachOrdered(target -> {
                    final var sanitizedTarget = sanitize(target);
                    graphviz.addTarget(sanitizedTarget, currentNode);
                    mermaid.addTarget(sanitizedTarget, currentNode);
                });
    }

    private static String sanitize(String name) {
        return name != null ? name.trim().replaceAll("\"", "") : null;
    }

    static final class Graphviz {
        String currentGraph = "";
        final List<String> nodes = new ArrayList<>();
        final List<String> edges = new ArrayList<>();
        final Map<String, List<String>> subGraphs = new TreeMap<>();

        // Structured mirror of the graph above, kept so it can be exposed as data
        // (for a client-side renderer) instead of only as a DOT string. Nodes are
        // deduplicated by id; a processor placeholder is upgraded when a more
        // specific role (source/sink/store) is later learned for the same id.
        final Map<String, Map<String, String>> structuredNodes = new LinkedHashMap<>();
        final List<Map<String, String>> structuredEdges = new ArrayList<>();

        private void node(String id, String label, String type, String subTopology) {
            final var existing = structuredNodes.get(id);
            if (existing == null) {
                final Map<String, String> n = new LinkedHashMap<>();
                n.put("id", id);
                n.put("label", label);
                n.put("type", type);
                n.put("subTopology", subTopology == null ? "" : subTopology);
                structuredNodes.put(id, n);
            } else {
                if ("processor".equals(existing.get("type")) && !"processor".equals(type)) {
                    existing.put("type", type);
                }
                if (existing.get("subTopology").isEmpty() && subTopology != null && !subTopology.isEmpty()) {
                    existing.put("subTopology", subTopology);
                }
            }
        }

        // Records a directed edge, ensuring both endpoints exist (as a plain
        // processor placeholder if not already typed) without overriding a role
        // or sub-topology already known for them.
        private void edge(String from, String to) {
            node(from, from, "processor", "");
            node(to, to, "processor", "");
            final Map<String, String> e = new LinkedHashMap<>();
            e.put("source", from);
            e.put("target", to);
            structuredEdges.add(e);
        }

        String toGraph() {
            final var res = new ArrayList<String>();

            res.add("digraph {");
            res.add(" fontname=Helvetica; fontsize=10;");
            res.add(" node [style=filled fillcolor=white color=\"#C9B7DD\" shape=box fontname=Helvetica fontsize=10];");
            nodes.forEach(n -> res.add(' ' + n + ';'));
            subGraphs.entrySet().forEach(e -> {
                res.add(" subgraph cluster" + e.getKey() + " {");
                res.add("  label=\"Sub-Topology: " + e.getKey() + "\"; color=\"#C8C879\"; bgcolor=\"#FFFFDE\";");
                e.getValue().forEach(v -> res.add("  " + v + ';'));
                res.add(" }");
            });
            edges.forEach(e -> res.add(' ' + e + ';'));
            res.add("}");

            return String.join("\n", res);
        }

        private void addSubTopology(String subTopology) {
            currentGraph = subTopology;
            subGraphs.put(subTopology, new ArrayList<>());
        }

        private void addSink(String sink, String topic) {
            nodes.add(toId(topic) + " [label=\"" + toLabel(topic) + "\" shape=house margin=\"0,0\"]");
            edges.add(toId(sink) + " -> " + toId(topic));
            node(topic, topic, "sink", "");
            edge(sink, topic);
        }

        private void addSource(String source, String topic) {
            nodes.add(toId(topic) + " [label=\"" + toLabel(topic) + "\" shape=invhouse margin=\"0,0\"]");
            nodes.add(toId(source) + " [label=\"" + toLabel(source) + "\"]");
            edges.add(toId(topic) + " -> " + toId(source));
            subGraphs.get(currentGraph).add(toId(source));
            node(topic, topic, "source", "");
            node(source, source, "processor", currentGraph);
            edge(topic, source);
        }

        private void addRegexSource(String source, String regex) {
            final var regexId = "REGEX_" + nodes.size();
            final var regexLabel = regex.replaceAll("\\\\", "\\\\\\\\");
            nodes.add(regexId + " [label=\"" + regexLabel + "\" shape=invhouse style=dashed margin=\"0,0\"]");
            nodes.add(toId(source) + " [label=\"" + toLabel(source) + "\"]");
            edges.add(regexId + " -> " + toId(source));
            subGraphs.get(currentGraph).add(toId(source));
            node(regexId, regex, "source", "");
            node(source, source, "processor", currentGraph);
            edge(regexId, source);
        }

        private void addTarget(String target, String node) {
            nodes.add(toId(target) + " [label=\"" + toLabel(target) + "\"]");
            edges.add(toId(node) + " -> " + toId(target));
            subGraphs.get(currentGraph).add(toId(target));
            node(target, target, "processor", currentGraph);
            edge(node, target);
        }

        private void addStore(String store, String node, boolean join) {
            nodes.add(toId(store) + " [label=\"" + toLabel(store) + "\" shape=cylinder]");
            node(store, store, "store", "");
            if (join) {
                edges.add(toId(store) + " -> " + toId(node));
                edge(store, node);
            } else {
                edges.add(toId(node) + " -> " + toId(store));
                edge(node, store);
            }
        }

        private static String toId(String name) {
            return '\"' + name + '\"';
        }

        private static String toLabel(String name) {
            return name.replaceAll("-", "\\\\n");
        }
    }

    static final class Mermaid {
        final List<String> endpoints = new ArrayList<>();
        final List<String> subTopologies = new ArrayList<>();

        String toGraph() {
            final var res = new ArrayList<String>();

            res.add("graph TD");
            endpoints.forEach(e -> res.add(' ' + e));
            subTopologies.forEach(s -> res.add(' ' + s));
            if (!subTopologies.isEmpty()) {
                res.add(" end");
            }

            return String.join("\n", res);
        }

        private void addSubTopology(String subTopology) {
            if (!subTopologies.isEmpty()) {
                subTopologies.add("end");
            }
            subTopologies.add("subgraph Sub-Topology: " + subTopology);
        }

        private void addSink(String sink, String topic) {
            endpoints.add(sink + '[' + toName(sink) + "] --> " + topic + '(' + topic + ')');
        }

        private void addSource(String source, String topic) {
            endpoints.add(topic + '[' + topic + "] --> " + source + '(' + toName(source) + ')');
        }

        private void addRegexSource(String source, String regex) {
            endpoints.add("REGEX_" + endpoints.size() + '[' + regex + "] --> " + source + '(' + toName(source) + ')');
        }

        private void addTarget(String target, String node) {
            subTopologies.add(' ' + node + '[' + toName(node) + "] --> " + target + '(' + toName(target) + ')');
        }

        private void addStore(String store, String node, boolean join) {
            if (join) {
                endpoints.add(store + '[' + toName(store) + "] --> " + node + '(' + toName(node) + ')');
            } else {
                endpoints.add(node + '[' + toName(node) + "] --> " + store + '(' + toName(store) + ')');
            }
        }

        private static String toName(String name) {
            return name.replaceAll("-", "-<br>");
        }
    }
}
