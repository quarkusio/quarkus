package io.quarkus.deployment.configuration.tracker;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.util.GlobUtil;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration tracking and dumping
 * <p>
 * Configuration options for application build time configuration usage tracking
 * and dumping.
 */
@ConfigMapping(prefix = "quarkus.config-tracking")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface ConfigTrackingConfig {

    /**
     * Whether configuration dumping is enabled
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * Directory in which the configuration dump should be stored.
     * If not configured the {@code .quarkus} directory under the project directory will be used.
     */
    Optional<Path> directory();

    /**
     * File in which the configuration dump should be stored. If not configured, the {@link #filePrefix} and
     * {@link #fileSuffix} will be used to generate the final file name.
     * If the configured file path is absolute, the {@link #directory} option will be ignored. Otherwise,
     * the path will be considered relative to the {@link #directory}.
     */
    Optional<Path> file();

    /**
     * File name prefix. This option will be ignored in case {@link #file} is configured.
     */
    @WithDefault("quarkus")
    String filePrefix();

    /**
     * File name suffix. This option will be ignored in case {@link #file} is configured.
     */
    @WithDefault("-config-dump")
    String fileSuffix();

    /**
     * A list of config properties that should be excluded from the report.
     * GLOB patterns could be used instead of property names.
     */
    Optional<List<String>> exclude();

    /**
     * Translates the value of {@link #exclude} to a list of {@link java.util.regex.Pattern}.
     *
     * @return list of patterns created from {@link #exclude}
     */
    default List<Pattern> getExcludePatterns() {
        return toPatterns(exclude());
    }

    /**
     * A list of config properties whose values should be hashed in the report.
     * The values will be hashed using SHA-512 algorithm.
     * GLOB patterns could be used instead of property names.
     */
    Optional<List<String>> hashOptions();

    /**
     * Translates the value of {@link #hashOptions()} to a list of {@link java.util.regex.Pattern}.
     *
     * @return list of patterns created from {@link #hashOptions()}
     */
    default List<Pattern> getHashOptionsPatterns() {
        return toPatterns(hashOptions());
    }

    static List<Pattern> toPatterns(Optional<List<String>> globs) {
        if (globs.isEmpty()) {
            return List.of();
        }
        var list = globs.get();
        final List<Pattern> patterns = new ArrayList<>(list.size());
        for (var s : list) {
            patterns.add(Pattern.compile(GlobUtil.toRegexPattern(s)));
        }
        return patterns;
    }

    /**
     * Whether to use a {@code ~} as an alias for user home directory in path values
     */
    @WithDefault("true")
    boolean useUserHomeAliasInPaths();
}
