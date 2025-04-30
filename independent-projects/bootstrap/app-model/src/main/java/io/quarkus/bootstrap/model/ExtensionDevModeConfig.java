package io.quarkus.bootstrap.model;

import java.io.Serializable;
import java.util.Set;

import io.quarkus.maven.dependency.ArtifactKey;

/**
 * Extension Dev mode configuration options
 */
public class ExtensionDevModeConfig implements Serializable {

    private final ArtifactKey extensionKey;
    private final JvmOptions jvmOptions;
    private final Set<String> lockJvmOptions;

    public ExtensionDevModeConfig(ArtifactKey extensionKey, JvmOptions jvmOptions, Set<String> lockDefaultJvmOptions) {
        this.extensionKey = extensionKey;
        this.jvmOptions = jvmOptions;
        this.lockJvmOptions = lockDefaultJvmOptions;
    }

    /**
     * Extension key
     *
     * @return extension key
     */
    public ArtifactKey getExtensionKey() {
        return extensionKey;
    }

    /**
     * JVM options that should be added to the command line launching an application in Dev mode.
     *
     * @return JVM options to be added to the command line launching an application in Dev mode
     */
    public JvmOptions getJvmOptions() {
        return jvmOptions;
    }

    /**
     * JVM options whose default values should not be overridden with values that otherwise would be recommended
     * as defaults for Quarkus dev mode by the Quarkus Maven and Gradle plugins.
     *
     * @return JVM options that shouldn't be overridden
     */
    public Set<String> getLockJvmOptions() {
        return lockJvmOptions;
    }
}
