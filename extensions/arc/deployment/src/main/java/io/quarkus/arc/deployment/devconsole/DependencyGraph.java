package io.quarkus.arc.deployment.devconsole;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public class DependencyGraph {

    public final Set<DevBeanInfo> nodes;
    public final Set<Link> links;
    public final int maxLevel;

    public DependencyGraph(Set<DevBeanInfo> nodes, Set<Link> links) {
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
        Set<DevBeanInfo> newNodes = new HashSet<>();
        Set<String> usedIds = new HashSet<>();
        for (Link link : links) {
            if (predicate.test(link)) {
                newLinks.add(link);
                usedIds.add(link.source);
                usedIds.add(link.target);
            }
        }
        // Now keep only nodes for which a link exists...
        for (DevBeanInfo node : nodes) {
            if (usedIds.contains(node.getId())) {
                newNodes.add(node);
            }
        }
        return new DependencyGraph(newNodes, newLinks);
    }

    public static class Link {

        static Link dependent(String source, String target, int level) {
            return new Link(source, target, level == 0 ? "directDependent" : "dependency", level);
        }

        static Link dependency(String source, String target, int level) {
            return new Link(source, target, level == 0 ? "directDependency" : "dependency", level);
        }

        static Link lookup(String source, String target, int level) {
            return new Link(source, target, "lookup", level);
        }

        static Link producer(String source, String target, int level) {
            return new Link(source, target, "producer", level);
        }

        public final String source;
        public final String target;
        public final String type;
        public final int level;

        public Link(String source, String target, String type, int level) {
            this.source = source;
            this.target = target;
            this.type = type;
            this.level = level;
        }

        @Override
        public int hashCode() {
            return Objects.hash(source, target);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Link other = (Link) obj;
            return Objects.equals(source, other.source) && Objects.equals(target, other.target);
        }

    }

}
