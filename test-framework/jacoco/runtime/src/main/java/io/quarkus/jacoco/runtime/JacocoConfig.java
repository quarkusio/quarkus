package io.quarkus.jacoco.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.jacoco")
public interface JacocoConfig {

    static final String JACOCO_QUARKUS_EXEC = "jacoco-quarkus.exec";
    static final String JACOCO_REPORT = "jacoco-report";
    static final String TARGET_JACOCO_QUARKUS_EXEC = "target/" + JACOCO_QUARKUS_EXEC;
    static final String TARGET_JACOCO_REPORT = "target/" + JACOCO_REPORT;

    /**
     * Whether or not the Jacoco extension is enabled.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The Jacoco data file.
     * The path can be relative (to the module) or absolute.
     */
    @ConfigDocDefault(TARGET_JACOCO_QUARKUS_EXEC)
    Optional<String> dataFile();

    /**
     * Whether to reuse ({@code true}) or delete ({@code false}) the Jacoco
     * data file on each run.
     */
    @WithDefault("false")
    boolean reuseDataFile();

    /**
     * If Quarkus should generate the Jacoco report
     */
    @WithDefault("true")
    boolean report();

    /**
     * Encoding of the generated reports.
     */
    @WithDefault("UTF-8")
    String outputEncoding();

    /**
     * Name of the root node HTML report pages.
     */
    @WithDefault("${quarkus.application.name}")
    Optional<String> title();

    /**
     * Footer text used in HTML report pages.
     */
    public Optional<String> footer();

    /**
     * Encoding of the source files.
     */
    @WithDefault("UTF-8")
    public String sourceEncoding();

    /**
     * A list of class files to include in the report. May use wildcard
     * characters (* and ?). When not specified everything will be included.
     * <p>
     * For instance:
     * <ul>
     * <li><code>&#42;&#42;/fo/&#42;&#42;/&#42;</code> targets all classes under fo and sub packages</li>
     * <li><code>&#42;&#42;/bar/&#42;</code> targets all classes directly under bar</li>
     * <li><code>&#42;&#42;/&#42;BAR&#42;.class</code> targets classes that contain BAR in their name regardless of path</li>
     * </ul>
     */
    @WithDefault("**")
    public List<String> includes();

    /**
     * A list of class files to exclude from the report. May use wildcard
     * characters (* and ?). When not specified nothing will be excluded.
     * <p>
     * For instance:
     * <ul>
     * <li><code>&#42;&#42;/fo/&#42;&#42;/&#42;</code> targets all classes under fo and sub packages</li>
     * <li><code>&#42;&#42;/bar/&#42;</code> targets all classes directly under bar</li>
     * <li><code>&#42;&#42;/&#42;BAR&#42;.class</code> targets classes that contain BAR in their name regardless of path</li>
     * </ul>
     */
    public Optional<List<String>> excludes();

    /**
     * The location of the report files.
     * The path can be relative (to the module) or absolute.
     */
    @ConfigDocDefault(TARGET_JACOCO_REPORT)
    public Optional<String> reportLocation();
}
