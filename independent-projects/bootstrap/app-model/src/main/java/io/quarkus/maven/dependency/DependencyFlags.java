package io.quarkus.maven.dependency;

public interface DependencyFlags {

    /* @formatter:off */
    public static final int OPTIONAL =                             0b00000000001;
    public static final int DIRECT =                               0b00000000010;
    public static final int RUNTIME_CP =                           0b00000000100;
    public static final int DEPLOYMENT_CP =                        0b00000001000;
    public static final int RUNTIME_EXTENSION_ARTIFACT =           0b00000010000;
    public static final int WORKSPACE_MODULE =                     0b00000100000;
    public static final int RELOADABLE =                           0b00001000000;
    // A top-level runtime extension artifact is either a direct
    // dependency or a first extension dependency on the branch
    // navigating from the root to leaves
    public static final int TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT = 0b00010000000;
    public static final int CLASSLOADER_PARENT_FIRST             = 0b00100000000;
    public static final int CLASSLOADER_RUNNER_PARENT_FIRST      = 0b01000000000;
    public static final int CLASSLOADER_LESSER_PRIORITY          = 0b10000000000;
    /* @formatter:on */

}
