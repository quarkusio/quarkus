package io.quarkus.deployment.builditem.nativeimage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that indicates that a set of resource paths defined by globs should be
 * included in the native image.
 * <p>
 * Globs passed to the {@code includeGlob*()} methods of the {@link Builder} are passed directly
 * to the native image builder. See {@link NativeConfig.ResourcesConfig#includes} for the supported glob syntax.
 * <p>
 * The globs are passed to the native image builder using {@code reachability-metadata.json}
 * (conforming to {@code reachability-metadata-schema-v1.2.0.json}).
 * <p>
 * Related build items:
 * <ul>
 * <li>Use {@link NativeImageResourceBuildItem} if you need to add a single resource
 * <li>Use {@link NativeImageResourceDirectoryBuildItem} if you need to add a directory of resources
 * </ul>
 */
public final class NativeImageResourcePatternsBuildItem extends MultiBuildItem {

    private final List<String> includeGlobs;
    private final String module;

    private NativeImageResourcePatternsBuildItem(List<String> includeGlobs, String module) {
        this.includeGlobs = includeGlobs;
        this.module = module;
    }

    public List<String> getIncludeGlobs() {
        return includeGlobs;
    }

    /**
     * This is useful also for resources from within the JDK itself. Think e.g. some i18n files.
     *
     * @return The Java module containing these resources, or null if on the unnamed module/classpath.
     */
    public String getModule() {
        return module;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<String> includeGlobs = new ArrayList<>();
        private String module;

        public NativeImageResourcePatternsBuildItem build() {
            final List<String> iGlobs = includeGlobs;
            includeGlobs = null;
            return new NativeImageResourcePatternsBuildItem(
                    Collections.unmodifiableList(iGlobs),
                    module);
        }

        /**
         * Specifies the Java module from which the resources should be taken (e.g., "java.desktop").
         * This is useful also for resources from within the JDK itself. Think e.g. some i18n files.
         *
         * @param module the module name
         * @return this {@link Builder}
         */
        public Builder module(String module) {
            this.module = module;
            return this;
        }

        /**
         * Adds a glob pattern to select resource paths that should be included in the native-image.
         * <p>
         * Use a forward slash ({@code /}) as a path separator on all platforms. Globs must not start
         * with a slash. See {@link NativeConfig.ResourcesConfig#includes} for the supported glob syntax.
         *
         * @param glob the glob pattern to add
         * @return this {@link Builder}
         */
        public Builder includeGlob(String glob) {
            includeGlobs.add(glob);
            return this;
        }

        /**
         * Adds a collection of glob patterns to include resources in the native-image.
         *
         * @param globs the glob patterns to add
         * @return this {@link Builder}
         */
        public Builder includeGlobs(Collection<String> globs) {
            includeGlobs.addAll(globs);
            return this;
        }

        /**
         * Adds multiple glob patterns to include resources in the native-image.
         *
         * @param globs the glob patterns to add
         * @return this {@link Builder}
         */
        public Builder includeGlobs(String... globs) {
            Collections.addAll(includeGlobs, globs);
            return this;
        }
    }
}
