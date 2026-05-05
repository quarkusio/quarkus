package io.quarkus.deployment.builditem.nativeimage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.util.GlobUtil;

/**
 * A build item that indicates that a set of resource paths defined by globs should be
 * included in the native image.
 * <p>
 * Globs passed to the {@code includeGlob*()} methods of the {@link Builder} are passed directly
 * to the native image builder. See {@link NativeConfig.ResourcesConfig#includes} for the supported glob syntax.
 * Note that legacy regular expression patterns and resource exclusions are no longer supported by the
 * underlying GraalVM reachability metadata schema.
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

    private static final Logger log = Logger.getLogger(NativeImageResourcePatternsBuildItem.class);

    @Deprecated(since = "3.29", forRemoval = true)
    private final List<String> excludePatterns;

    @Deprecated(since = "3.29", forRemoval = true)
    private final List<String> includePatterns;

    private final List<String> includeGlobs;
    private final String module;

    private NativeImageResourcePatternsBuildItem(List<String> includeGlobs, List<String> includePatterns,
            List<String> excludePatterns, String module) {
        this.includeGlobs = includeGlobs;
        this.includePatterns = includePatterns;
        this.excludePatterns = excludePatterns;
        this.module = module;
    }

    /**
     * @return the list of excluded patterns.
     * @deprecated Resource exclusion is not supported by {@code reachability-metadata.json}.
     */
    @Deprecated(since = "3.29", forRemoval = true)
    public List<String> getExcludePatterns() {
        return excludePatterns;
    }

    /**
     * @return the list of included regular expression patterns.
     * @deprecated Regular expressions are not supported by {@code reachability-metadata.json}. Use globs.
     */
    @Deprecated(since = "3.29", forRemoval = true)
    public List<String> getIncludePatterns() {
        return includePatterns;
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
        private List<String> excludePatterns = new ArrayList<>();
        private List<String> includePatterns = new ArrayList<>();
        private List<String> includeGlobs = new ArrayList<>();
        private String module;

        public NativeImageResourcePatternsBuildItem build() {
            final List<String> iGlobs = includeGlobs;
            includeGlobs = null;
            final List<String> iPat = includePatterns;
            includePatterns = null;
            final List<String> ePat = excludePatterns;
            excludePatterns = null;
            return new NativeImageResourcePatternsBuildItem(
                    Collections.unmodifiableList(iGlobs),
                    Collections.unmodifiableList(iPat),
                    Collections.unmodifiableList(ePat),
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

        private void errorExclude(String patternOrGlob) {
            log.errorf("Resource excludes are no longer supported by native-image's reachability-metadata.json. " +
                    "The exclude pattern '%s' ignored. Remove it from your configuration or extension.",
                    patternOrGlob);
        }

        /**
         * @deprecated Resource exclusion is not supported by {@code reachability-metadata.json}.
         */
        @Deprecated(since = "3.29", forRemoval = true)
        public Builder excludeGlob(String glob) {
            errorExclude(glob);
            excludePatterns.add(GlobUtil.toRegexPattern(glob));
            return this;
        }

        /**
         * @deprecated Resource exclusion is not supported by {@code reachability-metadata.json}.
         */
        @Deprecated(since = "3.29", forRemoval = true)
        public Builder excludeGlobs(Collection<String> globs) {
            for (String glob : globs) {
                errorExclude(glob);
                excludePatterns.add(GlobUtil.toRegexPattern(glob));
            }
            return this;
        }

        /**
         * @deprecated Resource exclusion is not supported by {@code reachability-metadata.json}.
         */
        @Deprecated(since = "3.29", forRemoval = true)
        public Builder excludeGlobs(String... globs) {
            for (String glob : globs) {
                errorExclude(glob);
                excludePatterns.add(GlobUtil.toRegexPattern(glob));
            }
            return this;
        }

        /**
         * @deprecated Resource exclusion is not supported by {@code reachability-metadata.json}.
         */
        @Deprecated(since = "3.29", forRemoval = true)
        public Builder excludePattern(String pattern) {
            errorExclude(pattern);
            excludePatterns.add(pattern);
            return this;
        }

        /**
         * @deprecated Resource exclusion is not supported by {@code reachability-metadata.json}.
         */
        @Deprecated(since = "3.29", forRemoval = true)
        public Builder excludePatterns(Collection<String> patterns) {
            for (String pattern : patterns) {
                errorExclude(pattern);
                excludePatterns.add(pattern);
            }
            return this;
        }

        /**
         * @deprecated Resource exclusion is not supported by {@code reachability-metadata.json}.
         */
        @Deprecated(since = "3.29", forRemoval = true)
        public Builder excludePatterns(String... patterns) {
            for (String pattern : patterns) {
                errorExclude(pattern);
                excludePatterns.add(pattern);
            }
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

        /**
         * @deprecated Regular expressions are not supported by {@code reachability-metadata.json}.
         *             Use {@link #includeGlob(String)}.
         */
        @Deprecated(since = "3.29", forRemoval = true)
        public Builder includePattern(String pattern) {
            includePatterns.add(pattern);
            return this;
        }

        /**
         * @deprecated Regular expressions are not supported by {@code reachability-metadata.json}.
         *             Use {@link #includeGlobs(Collection)}.
         */
        @Deprecated(since = "3.29", forRemoval = true)
        public Builder includePatterns(Collection<String> patterns) {
            includePatterns.addAll(patterns);
            return this;
        }

        /**
         * @deprecated Regular expressions are not supported by {@code reachability-metadata.json}.
         *             Use {@link #includeGlobs(String...)}.
         */
        @Deprecated(since = "3.29", forRemoval = true)
        public Builder includePatterns(String... patterns) {
            Collections.addAll(includePatterns, patterns);
            return this;
        }
    }
}
