package io.quarkus.vertx.http.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.vertx.core.http.ClientAuth;

@ConfigRoot(name = "http", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class HttpBuildTimeConfig {

    /**
     * The HTTP root path. All web content will be served relative to this root path.
     */
    @ConfigItem(defaultValue = "/")
    public String rootPath;

    public AuthConfig auth;

    /**
     * Configures the engine to require/request client authentication.
     * NONE, REQUEST, REQUIRED
     */
    @ConfigItem(name = "ssl.client-auth", defaultValue = "NONE")
    public ClientAuth tlsClientAuth;

    /**
     * If this is true then only a virtual channel will be set up for vertx web.
     * We have this switch for testing purposes.
     */
    @ConfigItem
    public boolean virtual;

    /**
     * The HTTP root path for non application endpoints. Various endpoints such as metrics, health,
     * and open api are deployed under this path.
     * Setting the value to "/" disables the separate non application root,
     * resulting in all non application endpoints being served from "/" along with the application.
     */
    @ConfigItem(defaultValue = "/q")
    public String nonApplicationRootPath;

    /**
     * Whether to redirect non application endpoints from previous location off the root to the
     * new non application root path.
     * Enabled by default.
     */
    @ConfigItem(defaultValue = "true")
    public boolean redirectToNonApplicationRootPath;

    public String adjustPath(String path) {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Path must start with /");
        }
        if (rootPath.equals("/")) {
            return path;
        }
        return rootPath + path;
    }
}
