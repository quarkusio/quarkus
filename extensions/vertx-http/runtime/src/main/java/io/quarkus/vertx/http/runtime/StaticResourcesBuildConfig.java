package io.quarkus.vertx.http.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class StaticResourcesBuildConfig {

    /**
     * Whether to map directories from the local file system onto a specified path.
     * <p>
     * The key corresponds to the relative path of the directory on the file system, while the value corresponds
     * to the HTTP path under which the resource should be served.
     */
    @ConfigItem
    public Map<String, String> additionalLocalDirectories;

}
