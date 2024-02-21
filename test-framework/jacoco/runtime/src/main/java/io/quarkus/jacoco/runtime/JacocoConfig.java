package io.quarkus.jacoco.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class JacocoConfig {

    public static final String JACOCO_QUARKUS_EXEC = "jacoco-quarkus.exec";
    public static final String JACOCO_REPORT = "jacoco-report";
    public static final String TARGET_JACOCO_QUARKUS_EXEC = "target/" + JACOCO_QUARKUS_EXEC;
    public static final String TARGET_JACOCO_REPORT = "target/" + JACOCO_REPORT;

    /**
     * Whether or not the jacoco extension is enabled. Disabling it can come in handy when runnig tests in IDEs that do their
     * own jacoco instrumentation, e.g. EclEmma in Eclipse.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * The jacoco data file.
     * The path can be relative (to the module) or absolute.
     */
    @ConfigItem
    @ConfigDocDefault(TARGET_JACOCO_QUARKUS_EXEC)
    public Optional<String> dataFile;

    /**
     * Whether to reuse ({@code true}) or delete ({@code false}) the jacoco
     * data file on each run.
     */
    @ConfigItem(defaultValue = "false")
    public boolean reuseDataFile;

    /**
     * If Quarkus should generate the Jacoco report
     */
    @ConfigItem(defaultValue = "true")
    public boolean report;

    /**
     * Encoding of the generated reports.
     */
    @ConfigItem(defaultValue = "UTF-8")
    public String outputEncoding;

    /**
     * Name of the root node HTML report pages.
     */
    @ConfigItem
    public Optional<String> title;

    /**
     * Footer text used in HTML report pages.
     */
    @ConfigItem
    public Optional<String> footer;

    /**
     * Encoding of the source files.
     */
    @ConfigItem(defaultValue = "UTF-8")
    public String sourceEncoding;

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
    @ConfigItem(defaultValue = "**")
    public List<String> includes;

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
    @ConfigItem
    public Optional<List<String>> excludes;

    /**
     * The location of the report files.
     * The path can be relative (to the module) or absolute.
     */
    @ConfigItem
    @ConfigDocDefault(TARGET_JACOCO_REPORT)
    public Optional<String> reportLocation;
}
