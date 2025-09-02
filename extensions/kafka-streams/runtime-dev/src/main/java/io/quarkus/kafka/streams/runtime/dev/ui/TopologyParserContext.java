package io.quarkus.kafka.streams.runtime.dev.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

final class TopologyParserContext {
    String currentNode = "";
    final Set<String> subTopologies = new TreeSet<>();
    final Set<String> sources = new TreeSet<>();
    final Set<String> sinks = new TreeSet<>();
    final Set<String> stores = new TreeSet<>();
    final Graphviz graphviz = new Graphviz();
    final Mermaid mermaid = new Mermaid();

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
        }

        private void addSource(String source, String topic) {
            nodes.add(toId(topic) + " [label=\"" + toLabel(topic) + "\" shape=invhouse margin=\"0,0\"]");
            nodes.add(toId(source) + " [label=\"" + toLabel(source) + "\"]");
            edges.add(toId(topic) + " -> " + toId(source));
            subGraphs.get(currentGraph).add(toId(source));
        }

        private void addRegexSource(String source, String regex) {
            final var regexId = "REGEX_" + nodes.size();
            final var regexLabel = regex.replaceAll("\\\\", "\\\\\\\\");
            nodes.add(regexId + " [label=\"" + regexLabel + "\" shape=invhouse style=dashed margin=\"0,0\"]");
            nodes.add(toId(source) + " [label=\"" + toLabel(source) + "\"]");
            edges.add(regexId + " -> " + toId(source));
            subGraphs.get(currentGraph).add(toId(source));
        }

        private void addTarget(String target, String node) {
            nodes.add(toId(target) + " [label=\"" + toLabel(target) + "\"]");
            edges.add(toId(node) + " -> " + toId(target));
            subGraphs.get(currentGraph).add(toId(target));
        }

        private void addStore(String store, String node, boolean join) {
            nodes.add(toId(store) + " [label=\"" + toLabel(store) + "\" shape=cylinder]");
            if (join) {
                edges.add(toId(store) + " -> " + toId(node));
            } else {
                edges.add(toId(node) + " -> " + toId(store));
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
