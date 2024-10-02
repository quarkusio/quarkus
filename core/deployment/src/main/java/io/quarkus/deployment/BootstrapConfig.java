package io.quarkus.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Bootstrap
 * <p>
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
     * If set to true, workspace loader will log warnings for modules that could not be loaded for some reason
     * instead of throwing errors.
     */
    @ConfigItem(defaultValue = "false")
    boolean warnOnFailingWorkspaceModules;

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
     * A temporary option introduced to avoid a logging warning when {@code -Dquarkus.bootstrap.incubating-model-resolver}
     * is added to the build command line.
     * This option enables an incubating implementation of the Quarkus Application Model resolver.
     * This option will be removed as soon as the incubating implementation becomes the default one.
     */
    @ConfigItem(defaultValue = "false")
    boolean incubatingModelResolver;

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
