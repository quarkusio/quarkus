package org.jboss.shamrock.undertow;

import java.util.ArrayList;
import java.util.List;

import org.jboss.builder.item.MultiBuildItem;

public final class ServletBuildItem extends MultiBuildItem {

    private final String name;
    private final String servletClass;
    private int loadOnStartup;
    private boolean asyncSupported;
    private final List<String> mappings = new ArrayList<>();

    public ServletBuildItem(String name, String servletClass) {
        this.name = name;
        this.servletClass = servletClass;
    }

    public String getName() {
        return name;
    }

    public String getServletClass() {
        return servletClass;
    }

    public List<String> getMappings() {
        return mappings;
    }

    public int getLoadOnStartup() {
        return loadOnStartup;
    }

    public ServletBuildItem setLoadOnStartup(int loadOnStartup) {
        this.loadOnStartup = loadOnStartup;
        return this;
    }

    public ServletBuildItem addMapping(String mappingPath) {
        mappings.add(mappingPath);
        return this;
    }

    public boolean isAsyncSupported() {
        return asyncSupported;
    }

    public ServletBuildItem setAsyncSupported(boolean asyncSupported) {
        this.asyncSupported = asyncSupported;
        return this;
    }
}
