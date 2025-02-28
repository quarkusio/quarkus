package io.quarkus.arc.deployment.devui;

import java.util.Objects;

public class Link {

    static Link dependency(String source, String target, int level) {
        return new Link(source, target, "dependency", level);
    }

    static Link lookup(String source, String target, int level) {
        return new Link(source, target, "lookup", level);
    }

    static Link builtin(String source, String target, int level) {
        return new Link(source, target, "builtin", level);
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
