package io.quarkus.maven.dependency;

public interface DependencyFlags {

    /* @formatter:off */
    public static final int OPTIONAL =                   0b0000001;
    public static final int DIRECT =                     0b0000010;
    public static final int RUNTIME_CP =                 0b0000100;
    public static final int DEPLOYMENT_CP =              0b0001000;
    public static final int RUNTIME_EXTENSION_ARTIFACT = 0b0010000;
    public static final int WORKSPACE_MODULE =           0b0100000;
    public static final int RELOADABLE =                 0b1000000;
    /* @formatter:on */

}
