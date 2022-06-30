package io.quarkus.arc.deployment.devconsole;

import java.util.Objects;
import java.util.Set;

public class DependecyGraph {

    public final Set<DevBeanInfo> nodes;
    public final Set<Link> links;

    public DependecyGraph(Set<DevBeanInfo> nodes, Set<Link> links) {
        this.nodes = nodes;
        this.links = links;
    }

    public static class Link {

        static Link dependent(boolean direct, String source, String target) {
            return new Link(source, target, direct ? "directDependent" : "dependency");
        }

        static Link dependency(boolean direct, String source, String target) {
            return new Link(source, target, direct ? "directDependency" : "dependency");
        }

        static Link lookup(String source, String target) {
            return new Link(source, target, "lookup");
        }

        static Link producer(String source, String target) {
            return new Link(source, target, "producer");
        }

        public final String source;
        public final String target;
        public final String type;

        public Link(String source, String target, String type) {
            this.source = source;
            this.target = target;
            this.type = type;
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
