/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.undertow;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.DispatcherType;

import org.jboss.builder.item.MultiBuildItem;

public final class FilterBuildItem extends MultiBuildItem {

    private final String name;
    private final String filterClass;
    private int loadOnStartup;
    private boolean asyncSupported;
    private final List<FilterMappingInfo> mappings = new ArrayList<>();

    public FilterBuildItem(String name, String filterClass) {
        this.name = name;
        this.filterClass = filterClass;
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

    public FilterBuildItem setLoadOnStartup(int loadOnStartup) {
        this.loadOnStartup = loadOnStartup;
        return this;
    }

    public FilterBuildItem addMapping(FilterMappingInfo mappingPath) {
        mappings.add(mappingPath);
        return this;
    }

    public boolean isAsyncSupported() {
        return asyncSupported;
    }

    public FilterBuildItem setAsyncSupported(boolean asyncSupported) {
        this.asyncSupported = asyncSupported;
        return this;
    }

    public FilterBuildItem addFilterUrlMapping(final String mapping, DispatcherType dispatcher) {
        mappings.add(new FilterMappingInfo(FilterMappingInfo.MappingType.URL, mapping, dispatcher));
        return this;
    }

    public FilterBuildItem addFilterServletNameMapping(final String mapping, DispatcherType dispatcher) {
        mappings.add(new FilterMappingInfo(FilterMappingInfo.MappingType.SERVLET, mapping, dispatcher));
        return this;
    }

    public FilterBuildItem insertFilterUrlMapping(final int pos, final String mapping, DispatcherType dispatcher) {
        mappings.add(pos, new FilterMappingInfo(FilterMappingInfo.MappingType.URL, mapping, dispatcher));
        return this;
    }

    public FilterBuildItem insertFilterServletNameMapping(final int pos, final String filterName, final String mapping,
            DispatcherType dispatcher) {
        mappings.add(pos, new FilterMappingInfo(FilterMappingInfo.MappingType.SERVLET, mapping, dispatcher));
        return this;
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
            URL, SERVLET;
        }

    }

}
