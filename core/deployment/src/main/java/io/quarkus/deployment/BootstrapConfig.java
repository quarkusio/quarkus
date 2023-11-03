package io.quarkus.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * This is used currently only to suppress warnings about unknown properties
 * when the user supplies something like: -Dquarkus.debug.reflection=true
 */
@ConfigRoot
public class BootstrapConfig {

    /**
     * If set to true, the workspace initialization will be based on the effective POMs
     * (i.e. properly interpolated, including support for profiles) instead of the raw ones.
     */
    @ConfigItem(defaultValue = "false")
    boolean effectiveModelBuilder;

    /**
     * If set to true, workspace discovery will be enabled for all launch modes.
     * Usually, workspace discovery is enabled by default only for dev and test modes.
     */
    @ConfigItem(defaultValue = "false")
    Boolean workspaceDiscovery;

    /**
     * By default, the bootstrap mechanism will create a shared cache of open JARs for
     * Quarkus classloaders to reduce the total number of opened ZIP FileSystems in dev and test modes.
     * Setting system property {@code quarkus.bootstrap.disable-jar-cache} to {@code true} will make
     * Quarkus classloaders create a new ZIP FileSystem for each JAR classpath element every time it is added
     * to a Quarkus classloader.
     */
    @ConfigItem(defaultValue = "false")
    boolean disableJarCache;

    /**
     * Whether to throw an error, warn or silently ignore misaligned platform BOM imports
     */
    @ConfigItem(defaultValue = "error")
    public MisalignedPlatformImports misalignedPlatformImports;

    public enum MisalignedPlatformImports {
        ERROR,
        WARN,
        IGNORE;
    }
}
