
package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class MountConfig {

    /**
     * The name of the volumeName to mount.
     *
     * @return The name.
     */
    @ConfigItem
    Optional<String> name;

    /**
     * The path to mount.
     *
     * @return The path.
     */
    @ConfigItem
    Optional<String> path;

    /**
     * Path within the volumeName from which the container's volumeName should be
     * mounted.
     *
     * @return The subPath.
     */
    @ConfigItem
    Optional<String> subPath;

    /**
     * ReadOnly
     *
     * @return True if mount is readonly, False otherwise.
     */
    @ConfigItem
    boolean readOnly;
}
