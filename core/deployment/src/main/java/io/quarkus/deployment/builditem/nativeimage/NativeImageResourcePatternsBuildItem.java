package io.quarkus.deployment.builditem.nativeimage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.util.GlobUtil;

/**
 * A build item that indicates that a set of resource paths defined by regular expression patterns or globs should be
 * included in the native image.
 * <p>
 * Globs passed to the {@code includeGlob*()} methods of the {@link Builder} are transformed to regular expressions
 * internally. See {@link NativeConfig.ResourcesConfig#includes} for the supported glob syntax.
 * <p>
 * The patterns are passed to the native image builder using {@code resource-config.json}.
 * The same mechanism (and regular expression syntax) is used by {@code native-image}'s
 * {@code -H:ResourceConfigurationFiles}, {@code -H:IncludeResources} and {@code -H:ExcludeResources} (since
 * GraalVM 20.3.0) command line options.
 * <p>
 * Related build items:
 * <ul>
 * <li>Use {@link NativeImageResourceBuildItem} if you need to add a single resource
 * <li>Use {@link NativeImageResourceDirectoryBuildItem} if you need to add a directory of resources
 * </ul>
 */
public final class NativeImageResourcePatternsBuildItem extends MultiBuildItem {
    private final List<String> includePatterns;

    private NativeImageResourcePatternsBuildItem(List<String> includePatterns) {
        this.includePatterns = includePatterns;
    }

    public List<String> getIncludePatterns() {
        return includePatterns;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<String> includePatterns = new ArrayList<>();

        public NativeImageResourcePatternsBuildItem build() {
            final List<String> incl = includePatterns;
            includePatterns = null;
            return new NativeImageResourcePatternsBuildItem(Collections.unmodifiableList(incl));
        }

        /**
         * Add a glob pattern for matching resource paths that should be added to the native image.
         * <p>
         * Use slash ({@code /}) as a path separator on all platforms. Globs must not start with slash. See
         * {@link NativeConfig.ResourcesConfig#includes} for the supported glob syntax.
         *
         * @param glob the glob pattern to add
         * @return this {@link Builder}
         */
        public Builder includeGlob(String glob) {
            includePatterns.add(GlobUtil.toRegexPattern(glob));
            return this;
        }

        /**
         * Add a collection of glob patterns for matching resource paths that should be added to the native image.
         * <p>
         * Use slash ({@code /}) as a path separator on all platforms. Globs must not start with slash. See
         * {@link NativeConfig.ResourcesConfig#includes} for the supported glob syntax.
         *
         * @param globs the glob patterns to add
         * @return this {@link Builder}
         */
        public Builder includeGlobs(Collection<String> globs) {
            globs.stream().map(GlobUtil::toRegexPattern).forEach(includePatterns::add);
            return this;
        }

        /**
         * Add an array of glob patterns for matching resource paths that should be added to the native image.
         * <p>
         * Use slash ({@code /}) as a path separator on all platforms. Globs must not start with slash. See
         * {@link NativeConfig.ResourcesConfig#includes} for the supported glob syntax.
         *
         * @param globs the glob patterns to add
         * @return this {@link Builder}
         */
        public Builder includeGlobs(String... globs) {
            Stream.of(globs).map(GlobUtil::toRegexPattern).forEach(includePatterns::add);
            return this;
        }
    }
}
