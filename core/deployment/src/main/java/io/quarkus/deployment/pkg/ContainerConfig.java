
package io.quarkus.deployment.pkg;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class ContainerConfig {

    /**
     * Flag that specifies if container build is enabled
     */
    @ConfigItem
    public boolean build;

    /**
     * Flag that specifies if container deploy is enabled
     */
    @ConfigItem
    public boolean deploy;
}
