package io.quarkus.maven.dependency;

public interface DependencyFlags {

    /* @formatter:off */
    int OPTIONAL =                             0b00000000000001;
    int DIRECT =                               0b00000000000010;
    int RUNTIME_CP =                           0b00000000000100;
    int DEPLOYMENT_CP =                        0b00000000001000;
    int RUNTIME_EXTENSION_ARTIFACT =           0b00000000010000;
    int WORKSPACE_MODULE =                     0b00000000100000;
    int RELOADABLE =                           0b00000001000000;
    // A top-level runtime extension artifact is either a direct
    // dependency or a first extension dependency on the branch
    // navigating from the root to leaves
    int TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT = 0b00000010000000;
    int CLASSLOADER_PARENT_FIRST             = 0b00000100000000;
    int CLASSLOADER_RUNNER_PARENT_FIRST      = 0b00001000000000;
    int CLASSLOADER_LESSER_PRIORITY          = 0b00010000000000;
    // General purpose flag that could be re-used for various
    // kinds of processing indicating that a dependency has been
    // visited. This flag is meant to be cleared for all the nodes
    // once the processing of the whole tree has completed.
    int VISITED                              = 0b00100000000000;

    /**
     * Compile-only dependencies are those that are configured
     * to be included only for the compile phase ({@code provided} dependency scope in Maven,
     * {@code compileOnly} configuration in Gradle).
     * <p>
     * These dependencies will not be present on the Quarkus application runtime or
     * augmentation (deployment) classpath when the application is bootstrapped in production mode
     * ({@code io.quarkus.runtime.LaunchMode.NORMAL}).
     * <p>
     * In Maven projects, compile-only dependencies will be present on both the runtime and the augmentation classpath
     * of a Quarkus application launched in dev and test modes, since {@code provided} dependencies are included
     * in the test classpath by Maven.
     * <p>
     * In Gradle projects, compile-only dependencies will be present on both the runtime and the augmentation classpath
     * of a Quarkus application launched in dev modes only.
     * <p>
     * In any case though, these dependencies will be available during augmentation for processing
     * using {@link io.quarkus.bootstrap.model.ApplicationModel#getDependencies(int)} by passing
     * this flag as an argument.
     */
    int COMPILE_ONLY                         = 0b01000000000000;
    /* @formatter:on */

}
