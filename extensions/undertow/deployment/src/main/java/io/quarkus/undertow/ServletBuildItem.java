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
