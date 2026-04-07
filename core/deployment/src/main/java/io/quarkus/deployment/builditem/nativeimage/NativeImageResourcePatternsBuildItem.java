package io.quarkus.deployment.builditem.nativeimage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.util.GlobUtil;

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

    @Deprecated(since = "3.29", forRemoval = true)
    public List<String> getExcludePatterns() {
        return excludePatterns;
    }

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

        private void warnExclude(String patternOrGlob) {
            log.warnf("Resource excludes are no longer supported by GraalVM 25.0+ reachability-metadata.json. " +
                    "The exclude pattern '%s' ignored. Remove it from your configuration or extension.",
                    patternOrGlob);
        }

        @Deprecated(since = "3.29", forRemoval = true)
        public Builder excludeGlob(String glob) {
            warnExclude(glob);
            excludePatterns.add(GlobUtil.toRegexPattern(glob));
            return this;
        }

        @Deprecated(since = "3.29", forRemoval = true)
        public Builder excludeGlobs(Collection<String> globs) {
            for (String glob : globs) {
                warnExclude(glob);
                excludePatterns.add(GlobUtil.toRegexPattern(glob));
            }
            return this;
        }

        @Deprecated(since = "3.29", forRemoval = true)
        public Builder excludeGlobs(String... globs) {
            for (String glob : globs) {
                warnExclude(glob);
                excludePatterns.add(GlobUtil.toRegexPattern(glob));
            }
            return this;
        }

        @Deprecated(since = "3.29", forRemoval = true)
        public Builder excludePattern(String pattern) {
            warnExclude(pattern);
            excludePatterns.add(pattern);
            return this;
        }

        @Deprecated(since = "3.29", forRemoval = true)
        public Builder excludePatterns(Collection<String> patterns) {
            for (String pattern : patterns) {
                warnExclude(pattern);
                excludePatterns.add(pattern);
            }
            return this;
        }

        @Deprecated(since = "3.29", forRemoval = true)
        public Builder excludePatterns(String... patterns) {
            for (String pattern : patterns) {
                warnExclude(pattern);
                excludePatterns.add(pattern);
            }
            return this;
        }

        public Builder includeGlob(String glob) {
            includeGlobs.add(glob);
            return this;
        }

        public Builder includeGlobs(Collection<String> globs) {
            includeGlobs.addAll(globs);
            return this;
        }

        public Builder includeGlobs(String... globs) {
            Collections.addAll(includeGlobs, globs);
            return this;
        }

        @Deprecated(since = "3.29", forRemoval = true)
        public Builder includePattern(String pattern) {
            includePatterns.add(pattern);
            return this;
        }

        @Deprecated(since = "3.29", forRemoval = true)
        public Builder includePatterns(Collection<String> patterns) {
            includePatterns.addAll(patterns);
            return this;
        }

        @Deprecated(since = "3.29", forRemoval = true)
        public Builder includePatterns(String... patterns) {
            Collections.addAll(includePatterns, patterns);
            return this;
        }
    }
}
