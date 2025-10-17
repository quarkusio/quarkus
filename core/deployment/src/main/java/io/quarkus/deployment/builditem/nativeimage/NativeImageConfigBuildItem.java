package io.quarkus.deployment.builditem.nativeimage;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item used to aggregate configuration settings for the GraalVM native image build.
 * <p>
 * This is a {@code MultiBuildItem}, meaning multiple instances can be produced by different extensions
 * during the build process.
 * It collects information such as:
 * <ul>
 * <li>Classes to be (re-)initialized at runtime</li>
 * <li>Resource bundles to include</li>
 * <li>Dynamic proxy definitions</li>
 * <li>System properties to be set within the native image</li>
 * </ul>
 * The final native image configuration is assembled by combining all produced instances of this build item.
 * Use the {@link #builder()} method to construct instances.
 */
public final class NativeImageConfigBuildItem extends MultiBuildItem {

    private final Set<String> runtimeInitializedClasses;
    private final Set<String> resourceBundles;
    private final Set<List<String>> proxyDefinitions;
    private final Map<String, String> nativeImageSystemProperties;

    public NativeImageConfigBuildItem(Builder builder) {
        this.runtimeInitializedClasses = Collections.unmodifiableSet(builder.runtimeInitializedClasses);
        this.resourceBundles = Collections.unmodifiableSet(builder.resourceBundles);
        this.proxyDefinitions = Collections.unmodifiableSet(builder.proxyDefinitions);
        this.nativeImageSystemProperties = Collections.unmodifiableMap(builder.nativeImageSystemProperties);
    }

    public Iterable<String> getRuntimeInitializedClasses() {
        return runtimeInitializedClasses;
    }

    /**
     * @deprecated Starting with Mandrel/GraalVM 23.1 for JDK 21 this is functionally the same with
     *             {@link #getRuntimeInitializedClasses()}.
     */
    @Deprecated(since = "3.30", forRemoval = true)
    public Iterable<String> getRuntimeReinitializedClasses() {
        return getRuntimeInitializedClasses();
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
        final Set<String> resourceBundles = new HashSet<>();
        final Set<List<String>> proxyDefinitions = new HashSet<>();
        final Map<String, String> nativeImageSystemProperties = new HashMap<>();

        public Builder addRuntimeInitializedClass(String className) {
            runtimeInitializedClasses.add(className);
            return this;
        }

        /**
         * @deprecated Starting with Mandrel/GraalVM 23.1 for JDK 21 this is functionally the same with
         *             {@link Builder#addRuntimeInitializedClass(String)}.
         */
        @Deprecated(since = "3.30", forRemoval = true)
        public Builder addRuntimeReinitializedClass(String className) {
            return addRuntimeInitializedClass(className);
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
