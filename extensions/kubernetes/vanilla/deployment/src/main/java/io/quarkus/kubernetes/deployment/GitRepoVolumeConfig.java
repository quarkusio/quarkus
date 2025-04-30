package io.quarkus.kubernetes.deployment;

import java.util.Optional;

public interface GitRepoVolumeConfig {
    /**
     * Git repository URL.
     */
    String repository();

    /**
     * The directory of the repository to mount.
     */
    Optional<String> directory();

    /**
     * The commit hash to use.
     */
    Optional<String> revision();
}
