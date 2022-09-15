package io.quarkus.undertow.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;

import io.quarkus.builder.item.MultiBuildItem;
import io.undertow.servlet.api.InstanceFactory;

public final class ServletBuildItem extends MultiBuildItem {

    private final String name;
    private final String servletClass;
    private final int loadOnStartup;
    private final boolean asyncSupported;
    private final List<String> mappings;
    private final InstanceFactory<? extends Servlet> instanceFactory;
    private final Map<String, String> initParams;
    private final MultipartConfigElement multipartConfig;

    private ServletBuildItem(Builder builder) {
        this.name = builder.name;
        this.servletClass = builder.servletClass;
        this.loadOnStartup = builder.loadOnStartup;
        this.asyncSupported = builder.asyncSupported;
        this.mappings = List.copyOf(builder.mappings);
        this.instanceFactory = builder.instanceFactory;
        this.initParams = Collections.unmodifiableMap(new HashMap<>(builder.initParams));
        this.multipartConfig = builder.multipartConfig;
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

    public boolean isAsyncSupported() {
        return asyncSupported;
    }

    public Map<String, String> getInitParams() {
        return initParams;
    }

    public InstanceFactory<? extends Servlet> getInstanceFactory() {
        return instanceFactory;
    }

    public MultipartConfigElement getMultipartConfig() {
        return multipartConfig;
    }

    public static Builder builder(String name, String servletClass) {
        return new Builder(name, servletClass);
    }

    public static class Builder {
        private final String name;
        private final String servletClass;
        private int loadOnStartup;
        private boolean asyncSupported;
        private List<String> mappings = new ArrayList<>();
        private InstanceFactory<? extends Servlet> instanceFactory;
        private Map<String, String> initParams = new HashMap<>();
        private MultipartConfigElement multipartConfig;

        Builder(String name, String servletClass) {
            this.name = name;
            this.servletClass = servletClass;
        }

        public String getName() {
            return name;
        }

        public String getServletClass() {
            return servletClass;
        }

        public int getLoadOnStartup() {
            return loadOnStartup;
        }

        public Builder setLoadOnStartup(int loadOnStartup) {
            this.loadOnStartup = loadOnStartup;
            return this;
        }

        public boolean isAsyncSupported() {
            return asyncSupported;
        }

        public Builder setAsyncSupported(boolean asyncSupported) {
            this.asyncSupported = asyncSupported;
            return this;
        }

        public List<String> getMappings() {
            return mappings;
        }

        public Builder setMappings(List<String> mappings) {
            this.mappings = mappings;
            return this;
        }

        public InstanceFactory<? extends Servlet> getInstanceFactory() {
            return instanceFactory;
        }

        public Builder setInstanceFactory(InstanceFactory<? extends Servlet> instanceFactory) {
            this.instanceFactory = instanceFactory;
            return this;
        }

        public Map<String, String> getInitParams() {
            return initParams;
        }

        public Builder addMapping(String mappingPath) {
            mappings.add(mappingPath);
            return this;
        }

        public Builder addInitParam(String key, String value) {
            initParams.put(key, value);
            return this;
        }

        public MultipartConfigElement getMultipartConfig() {
            return multipartConfig;
        }

        public Builder setMultipartConfig(MultipartConfigElement multipartConfig) {
            this.multipartConfig = multipartConfig;
            return this;
        }

        public ServletBuildItem build() {
            return new ServletBuildItem(this);
        }
    }
}
