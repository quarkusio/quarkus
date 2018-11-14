package org.jboss.shamrock.deployment.builditem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.builder.item.MultiBuildItem;

public final class SubstrateConfigBuildItem extends MultiBuildItem {

    private final List<String> runtimeInitializedClasses;
    private final List<String> resourceBundles;
    private final Set<String> proxyDefinitions;
    private final Map<String, String> nativeImageSystemProperties;

    public SubstrateConfigBuildItem(Builder builder) {
        this.runtimeInitializedClasses = Collections.unmodifiableList(builder.runtimeInitializedClasses);
        this.resourceBundles = Collections.unmodifiableList(builder.resourceBundles);
        this.proxyDefinitions = Collections.unmodifiableSet(builder.proxyDefinitions);
        this.nativeImageSystemProperties = Collections.unmodifiableMap(builder.nativeImageSystemProperties);
    }

    public Iterable<String> getRuntimeInitializedClasses() {
        return runtimeInitializedClasses;
    }

    public Iterable<String> getResourceBundles() {
        return resourceBundles;
    }

    public Iterable<String> getProxyDefinitions() {
        return proxyDefinitions;
    }

    public Map<String, String> getNativeImageSystemProperties() {
        return nativeImageSystemProperties;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        final List<String> runtimeInitializedClasses = new ArrayList<>();
        final List<String> resourceBundles = new ArrayList<>();
        final Set<String> proxyDefinitions = new HashSet<>();
        final Map<String, String> nativeImageSystemProperties = new HashMap<>();

        public Builder addRuntimeInitializedClass(String className) {
            runtimeInitializedClasses.add(className);
            return this;
        }

        public Builder addResourceBundle(String className) {
            resourceBundles.add(className);
            return this;
        }

        public Builder addProxyClassDefinition(String... classes) {
            for (String s : classes) {
                proxyDefinitions.add(s);
            }
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
