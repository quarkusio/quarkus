package io.quarkus.arc.deployment.devui;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class DependencyGraph {

    static final DependencyGraph EMPTY = new DependencyGraph(Set.of(), Set.of());

    public final Set<Node> nodes;
    public final Set<Link> links;
    public final int maxLevel;

    public DependencyGraph(Set<Node> nodes, Set<Link> links) {
        this.nodes = nodes;
        this.links = links;
        this.maxLevel = links.stream().mapToInt(l -> l.level).max().orElse(0);
    }

    DependencyGraph forLevel(int level) {
        return filterLinks(link -> link.level <= level);
    }

    DependencyGraph filterLinks(Predicate<Link> predicate) {
        // Filter out links first
        Set<Link> newLinks = new HashSet<>();
        Set<Node> newNodes = new HashSet<>();
        Set<String> usedIds = new HashSet<>();
        for (Link link : links) {
            if (predicate.test(link)) {
                newLinks.add(link);
                usedIds.add(link.source);
                usedIds.add(link.target);
            }
        }
        // Now keep only nodes for which a link exists...
        for (Node node : nodes) {
            if (usedIds.contains(node.getId())) {
                newNodes.add(node);
            }
        }
        return new DependencyGraph(newNodes, newLinks);
    }

}
