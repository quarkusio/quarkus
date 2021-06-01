package io.quarkus.vertx.http.runtime;

import java.time.Duration;

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
     * A common root path for non-application endpoints. Various extension-provided endpoints such as metrics, health,
     * and openapi are deployed under this path by default.
     *
     * * Relative path (Default, `q`) ->
     * Non-application endpoints will be served from
     * `${quarkus.http.root-path}/${quarkus.http.non-application-root-path}`.
     * * Absolute path (`/q`) ->
     * Non-application endpoints will be served from the specified path.
     * * `${quarkus.http.root-path}` -> Setting this path to the same value as HTTP root path disables
     * this root path. All extension-provided endpoints will be served from `${quarkus.http.root-path}`.
     * 
     * @asciidoclet
     */
    @ConfigItem(defaultValue = "q")
    public String nonApplicationRootPath;

    /**
     * The REST Assured client timeout for testing.
     */
    @ConfigItem(defaultValue = "30s")
    public Duration testTimeout;
}
