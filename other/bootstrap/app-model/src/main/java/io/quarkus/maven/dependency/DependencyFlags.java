package io.quarkus.maven.dependency;

public interface DependencyFlags {

    /* @formatter:off */
    public static final int OPTIONAL =                             0b000000000001;
    public static final int DIRECT =                               0b000000000010;
    public static final int RUNTIME_CP =                           0b000000000100;
    public static final int DEPLOYMENT_CP =                        0b000000001000;
    public static final int RUNTIME_EXTENSION_ARTIFACT =           0b000000010000;
    public static final int WORKSPACE_MODULE =                     0b000000100000;
    public static final int RELOADABLE =                           0b000001000000;
    // A top-level runtime extension artifact is either a direct
    // dependency or a first extension dependency on the branch
    // navigating from the root to leaves
    public static final int TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT = 0b000010000000;
    public static final int CLASSLOADER_PARENT_FIRST             = 0b000100000000;
    public static final int CLASSLOADER_RUNNER_PARENT_FIRST      = 0b001000000000;
    public static final int CLASSLOADER_LESSER_PRIORITY          = 0b010000000000;
    // General purpose flag that could be re-used for various
    // kinds of processing indicating that a dependency has been
    // visited. This flag is meant to be cleared for all the nodes
    // once the processing of the whole tree has completed.
    public static final int VISITED                              = 0b100000000000;
    /* @formatter:on */

}
