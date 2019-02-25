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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;

import org.jboss.builder.item.MultiBuildItem;

import io.undertow.servlet.api.InstanceFactory;

public final class ServletBuildItem extends MultiBuildItem {

    private final String name;
    private final String servletClass;
    private final int loadOnStartup;
    private final boolean asyncSupported;
    private final List<String> mappings;
    private final InstanceFactory<? extends Servlet> instanceFactory;
    private final Map<String, String> initParams;

    private ServletBuildItem(Builder builder) {
        this.name = builder.name;
        this.servletClass = builder.servletClass;
        this.loadOnStartup = builder.loadOnStartup;
        this.asyncSupported = builder.asyncSupported;
        this.mappings = Collections.unmodifiableList(new ArrayList<>(builder.mappings));
        this.instanceFactory = builder.instanceFactory;
        this.initParams = Collections.unmodifiableMap(new HashMap<>(builder.initParams));

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

        public ServletBuildItem build() {
            return new ServletBuildItem(this);
        }
    }
}
