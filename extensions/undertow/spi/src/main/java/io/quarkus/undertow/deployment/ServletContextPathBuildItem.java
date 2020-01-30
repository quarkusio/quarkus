package io.quarkus.undertow.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ServletContextPathBuildItem extends SimpleBuildItem {

    private final String servletContextPath;

    public ServletContextPathBuildItem(String servletContextPath) {
        this.servletContextPath = servletContextPath;
    }

    public String getServletContextPath() {
        return servletContextPath;
    }
}
