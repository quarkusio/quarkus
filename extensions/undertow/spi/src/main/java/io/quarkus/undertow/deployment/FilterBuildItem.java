package io.quarkus.undertow.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;

import io.quarkus.builder.item.MultiBuildItem;
import io.undertow.servlet.api.InstanceFactory;

public final class FilterBuildItem extends MultiBuildItem {

    private final String name;
    private final String filterClass;
    private final int loadOnStartup;
    private final boolean asyncSupported;
    private final List<FilterMappingInfo> mappings;
    private final InstanceFactory<? extends Filter> instanceFactory;
    private final Map<String, String> initParams;

    private FilterBuildItem(Builder builder) {
        this.name = builder.name;
        this.filterClass = builder.filterClass;
        this.loadOnStartup = builder.loadOnStartup;
        this.asyncSupported = builder.asyncSupported;
        this.mappings = List.copyOf(builder.mappings);
        this.instanceFactory = builder.instanceFactory;
        this.initParams = Collections.unmodifiableMap(new HashMap<>(builder.initParams));
    }

    public String getName() {
        return name;
    }

    public String getFilterClass() {
        return filterClass;
    }

    public List<FilterMappingInfo> getMappings() {
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

    public InstanceFactory<? extends Filter> getInstanceFactory() {
        return instanceFactory;
    }

    public static class FilterMappingInfo {

        private MappingType mappingType;
        private String mapping;
        private DispatcherType dispatcher;

        public FilterMappingInfo(final MappingType mappingType, final String mapping, final DispatcherType dispatcher) {
            this.mappingType = mappingType;
            this.mapping = mapping;
            this.dispatcher = dispatcher;
        }

        public void setMappingType(MappingType mappingType) {
            this.mappingType = mappingType;
        }

        public void setMapping(String mapping) {
            this.mapping = mapping;
        }

        public void setDispatcher(DispatcherType dispatcher) {
            this.dispatcher = dispatcher;
        }

        public MappingType getMappingType() {
            return mappingType;
        }

        public String getMapping() {
            return mapping;
        }

        public DispatcherType getDispatcher() {
            return dispatcher;
        }

        public enum MappingType {
            URL,
            SERVLET;
        }

    }

    public static Builder builder(String name, String filterClass) {
        return new Builder(name, filterClass);
    }

    public static class Builder {

        private final String name;
        private final String filterClass;
        private int loadOnStartup;
        private boolean asyncSupported;
        private final List<FilterMappingInfo> mappings = new ArrayList<>();
        private InstanceFactory<? extends Filter> instanceFactory;
        private final Map<String, String> initParams = new HashMap<>();

        public Builder(String name, String filterClass) {
            this.name = name;
            this.filterClass = filterClass;
        }

        public String getName() {
            return name;
        }

        public String getFilterClass() {
            return filterClass;
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

        public List<FilterMappingInfo> getMappings() {
            return mappings;
        }

        public InstanceFactory<? extends Filter> getInstanceFactory() {
            return instanceFactory;
        }

        public Builder setInstanceFactory(InstanceFactory<? extends Filter> instanceFactory) {
            this.instanceFactory = instanceFactory;
            return this;
        }

        public Map<String, String> getInitParams() {
            return initParams;
        }

        public Builder addMapping(FilterMappingInfo mappingPath) {
            mappings.add(mappingPath);
            return this;
        }

        public Builder addFilterUrlMapping(final String mapping, DispatcherType dispatcher) {
            mappings.add(new FilterMappingInfo(FilterMappingInfo.MappingType.URL, mapping, dispatcher));
            return this;
        }

        public Builder addFilterServletNameMapping(final String mapping, DispatcherType dispatcher) {
            mappings.add(new FilterMappingInfo(FilterMappingInfo.MappingType.SERVLET, mapping, dispatcher));
            return this;
        }

        public Builder insertFilterUrlMapping(final int pos, final String mapping, DispatcherType dispatcher) {
            mappings.add(pos, new FilterMappingInfo(FilterMappingInfo.MappingType.URL, mapping, dispatcher));
            return this;
        }

        public Builder insertFilterServletNameMapping(final int pos, final String filterName, final String mapping,
                DispatcherType dispatcher) {
            mappings.add(pos, new FilterMappingInfo(FilterMappingInfo.MappingType.SERVLET, mapping, dispatcher));
            return this;
        }

        public Builder addInitParam(String key, String value) {
            initParams.put(key, value);
            return this;
        }

        public FilterBuildItem build() {
            return new FilterBuildItem(this);
        }
    }

}
