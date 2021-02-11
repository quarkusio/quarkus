package io.quarkus.deployment.builditem.nativeimage;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

public final class NativeImageConfigBuildItem extends MultiBuildItem {

    private final Set<String> runtimeInitializedClasses;
    private final Set<String> runtimeReinitializedClasses;
    private final Set<String> resourceBundles;
    private final Set<List<String>> proxyDefinitions;
    private final Map<String, String> nativeImageSystemProperties;

    public NativeImageConfigBuildItem(Builder builder) {
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

        public NativeImageConfigBuildItem build() {
            return new NativeImageConfigBuildItem(this);
        }
    }

}
