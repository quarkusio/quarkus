package io.quarkus.kubernetes.spi;

public class RoleRef {
    private final boolean clusterWide;
    private final String name;

    public RoleRef(String name, boolean clusterWide) {
        this.name = name;
        this.clusterWide = clusterWide;
    }

    public boolean isClusterWide() {
        return clusterWide;
    }

    public String getName() {
        return name;
    }
}
