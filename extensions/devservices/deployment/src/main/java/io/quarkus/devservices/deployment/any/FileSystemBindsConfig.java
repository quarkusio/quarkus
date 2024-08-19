package io.quarkus.devservices.deployment.any;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;

@ConfigGroup
public interface FileSystemBindsConfig {

    /**
     * File System Binds
     */
    @ConfigDocMapKey("host-path")
    @WithParentName
    @WithDefaults
    Map<String, FileSystemBindConfig> fileSystemBind();

}
