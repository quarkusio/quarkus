package io.quarkus.jacoco.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class JacocoConfig {

    /**
     * The jacoco data file
     */
    @ConfigItem(defaultValue = "jacoco-quarkus.exec")
    public String dataFile;

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
     */
    @ConfigItem(defaultValue = "jacoco-report")
    public String reportLocation;
}
