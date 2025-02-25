package io.quarkus.deployment;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Bootstrap
 * <p>
 * This is used currently only to suppress warnings about unknown properties
 * when the user supplies something like: -Dquarkus.debug.reflection=true
 */
@ConfigMapping(prefix = "quarkus.bootstrap")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface BootstrapConfig {

    /**
     * If set to true, the workspace initialization will be based on the effective POMs
     * (i.e. properly interpolated, including support for profiles) instead of the raw ones.
     */
    @WithDefault("false")
    boolean effectiveModelBuilder();

    /**
     * If set to true, workspace discovery will be enabled for all launch modes.
     * Usually, workspace discovery is enabled by default only for dev and test modes.
     */
    @WithDefault("false")
    boolean workspaceDiscovery();

    /**
     * If set to true, workspace loader will log warnings for modules that could not be loaded for some reason
     * instead of throwing errors.
     */
    @WithDefault("false")
    boolean warnOnFailingWorkspaceModules();

    /**
     * By default, the bootstrap mechanism will create a shared cache of open JARs for
     * Quarkus classloaders to reduce the total number of opened ZIP FileSystems in dev and test modes.
     * Setting system property {@code quarkus.bootstrap.disable-jar-cache} to {@code true} will make
     * Quarkus classloaders create a new ZIP FileSystem for each JAR classpath element every time it is added
     * to a Quarkus classloader.
     */
    @WithDefault("false")
    boolean disableJarCache();

    /**
     * A temporary option introduced to avoid a logging warning when {@code -Dquarkus.bootstrap.legacy-model-resolver}
     * is added to the build command line.
     * This option enables the legacy implementation of the Quarkus Application Model resolver.
     * This option will be removed once the legacy {@link ApplicationModel} resolver implementation gets removed.
     */
    @WithDefault("false")
    boolean legacyModelResolver();

    /**
     * Whether to throw an error, warn or silently ignore misaligned platform BOM imports
     */
    @WithDefault("error")
    MisalignedPlatformImports misalignedPlatformImports();

    enum MisalignedPlatformImports {
        ERROR,
        WARN,
        IGNORE;
    }
}
