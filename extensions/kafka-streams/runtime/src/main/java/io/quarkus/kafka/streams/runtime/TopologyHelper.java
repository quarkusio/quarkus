package io.quarkus.kafka.streams.runtime;

import static org.apache.kafka.streams.TopologyDescription.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyDescription.Subtopology;

/**
 * Helper methods around KafkaStream Topology
 */
class TopologyHelper {

    static Set<String> discoverStaticTopics(Topology topology) {
        HashSet<String> sourceTopics = new HashSet<>();
        HashSet<String> sinkTopics = new HashSet<>();

        for (Subtopology subtopology : topology.describe().subtopologies()) {
            for (Node node : subtopology.nodes()) {
                if (node instanceof Source) {
                    Optional.ofNullable(((Source) node).topicSet()).ifPresent(sourceTopics::addAll);
                }
                if (node instanceof Sink) {
                    Optional.ofNullable(((Sink) node).topic()).ifPresent(sinkTopics::add);
                }
            }
        }

        // Ignore internal topics used to link sub-topologies together
        Set<String> internalTopics = new HashSet<String>(sourceTopics);
        internalTopics.retainAll(sinkTopics);
        sourceTopics.addAll(sinkTopics);
        sourceTopics.removeAll(internalTopics);
        return sourceTopics;
    }
}
