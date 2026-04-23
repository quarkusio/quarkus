package io.quarkus.signals.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.jboss.logging.Logger;

/**
 * Kahn's algorithm topological sort with alphabetical tiebreaker for deterministic ordering.
 */
class TopologicalSort {

    private static final Logger LOG = Logger.getLogger(TopologicalSort.class);

    /**
     * @param allIds all component identifiers
     * @param beforeEdges map of id to the list of ids it must come before
     * @param afterEdges map of id to the list of ids it must come after
     * @param componentTypeName the name of the component type, used in error messages
     * @return the ordered list of identifiers
     * @throws IllegalStateException if a cycle is detected
     */
    static List<String> sort(Set<String> allIds, Map<String, List<String>> beforeEdges,
            Map<String, List<String>> afterEdges, String componentTypeName) {
        Map<String, Set<String>> graph = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        for (String id : allIds) {
            graph.put(id, new HashSet<>());
            inDegree.put(id, 0);
        }

        // "A before B" means edge A -> B (skip if B is not present)
        for (Map.Entry<String, List<String>> entry : beforeEdges.entrySet()) {
            String from = entry.getKey();
            for (String to : entry.getValue()) {
                if (!allIds.contains(to)) {
                    LOG.debugf("%s '%s' declares @ComponentOrder(before = \"%s\") but no %s with @Identifier(\"%s\") exists",
                            componentTypeName, from, to, componentTypeName, to);
                    continue;
                }
                if (graph.get(from).add(to)) {
                    inDegree.merge(to, 1, Integer::sum);
                }
            }
        }

        // "A after B" means edge B -> A (skip if B is not present)
        for (Map.Entry<String, List<String>> entry : afterEdges.entrySet()) {
            String to = entry.getKey();
            for (String from : entry.getValue()) {
                if (!allIds.contains(from)) {
                    LOG.debugf("%s '%s' declares @ComponentOrder(after = \"%s\") but no %s with @Identifier(\"%s\") exists",
                            componentTypeName, to, from, componentTypeName, from);
                    continue;
                }
                if (graph.get(from).add(to)) {
                    inDegree.merge(to, 1, Integer::sum);
                }
            }
        }

        PriorityQueue<String> queue = new PriorityQueue<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<String> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String node = queue.poll();
            sorted.add(node);
            for (String neighbor : graph.get(node)) {
                int newDegree = inDegree.get(neighbor) - 1;
                inDegree.put(neighbor, newDegree);
                if (newDegree == 0) {
                    queue.add(neighbor);
                }
            }
        }

        if (sorted.size() != allIds.size()) {
            Set<String> remaining = new HashSet<>(allIds);
            remaining.removeAll(sorted);
            throw new IllegalStateException(
                    "Cycle detected in @ComponentOrder declarations involving " + componentTypeName + "s: " + remaining);
        }

        return sorted;
    }

}
