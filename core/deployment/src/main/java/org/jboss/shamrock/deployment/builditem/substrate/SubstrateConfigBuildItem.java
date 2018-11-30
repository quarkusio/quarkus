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

package org.jboss.shamrock.deployment.builditem.substrate;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.builder.item.MultiBuildItem;

public final class SubstrateConfigBuildItem extends MultiBuildItem {

    private final Set<String> runtimeInitializedClasses;
    private final Set<String> runtimeReinitializedClasses;
    private final Set<String> resourceBundles;
    private final Set<List<String>> proxyDefinitions;
    private final Map<String, String> nativeImageSystemProperties;

    public SubstrateConfigBuildItem(Builder builder) {
        this.runtimeInitializedClasses = Collections.unmodifiableSet(builder.runtimeInitializedClasses);
        this.runtimeReinitializedClasses = Collections.unmodifiableSet(builder.runtimeReinitializedClasses);
        this.resourceBundles = Collections.unmodifiableSet(builder.resourceBundles);
        this.proxyDefinitions = Collections.unmodifiableSet(builder.proxyDefinitions);
        this.nativeImageSystemProperties = Collections.unmodifiableMap(builder.nativeImageSystemProperties);
    }

    public Iterable<String> getRuntimeInitializedClasses() {
        return runtimeInitializedClasses;
    }

    public Iterable<String> getRuntimeReinitializedClasses() {
        return runtimeReinitializedClasses;
    }

    public Iterable<String> getResourceBundles() {
        return resourceBundles;
    }

    public Iterable<List<String>> getProxyDefinitions() {
        return proxyDefinitions;
    }

    public Map<String, String> getNativeImageSystemProperties() {
        return nativeImageSystemProperties;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        final Set<String> runtimeInitializedClasses = new HashSet<>();
        final Set<String> runtimeReinitializedClasses = new HashSet<>();
        final Set<String> resourceBundles = new HashSet<>();
        final Set<List<String>> proxyDefinitions = new HashSet<>();
        final Map<String, String> nativeImageSystemProperties = new HashMap<>();

        public Builder addRuntimeInitializedClass(String className) {
            runtimeInitializedClasses.add(className);
            return this;
        }

        public Builder addRuntimeReinitializedClass(String className) {
            runtimeReinitializedClasses.add(className);
            return this;
        }

        public Builder addResourceBundle(String className) {
            resourceBundles.add(className);
            return this;
        }

        public Builder addProxyClassDefinition(String... classes) {
            proxyDefinitions.add(Arrays.asList(classes));
            return this;
        }

        public Builder addNativeImageSystemProperty(String key, String value) {
            nativeImageSystemProperties.put(key, value);
            return this;
        }

        public SubstrateConfigBuildItem build() {
            return new SubstrateConfigBuildItem(this);
        }
    }

}
