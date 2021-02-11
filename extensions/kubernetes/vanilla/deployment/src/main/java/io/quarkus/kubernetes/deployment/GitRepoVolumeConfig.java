
package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class GitRepoVolumeConfig {

    /**
     * Git repoistory URL.
     */
    @ConfigItem
    String repository;

    /**
     * The directory of the repository to mount.
     */
    @ConfigItem
    Optional<String> directory;

    /**
     * The commit hash to use.
     */
    @ConfigItem
    Optional<String> revision;

}
