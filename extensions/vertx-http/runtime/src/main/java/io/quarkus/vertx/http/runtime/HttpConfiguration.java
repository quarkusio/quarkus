package io.quarkus.vertx.http.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * @deprecated Use {@link VertxHttpConfig}.
 */
@Deprecated(forRemoval = true, since = "3.19")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class HttpConfiguration {
    /**
     * The HTTP port
     */
    @ConfigItem(defaultValue = "8080", generateDocumentation = false)
    public int port;

    /**
     * The HTTP host
     * <p>
     * In dev/test mode this defaults to localhost, in prod mode this defaults to 0.0.0.0
     * <p>
     * Defaulting to 0.0.0.0 makes it easier to deploy Quarkus to container, however it
     * is not suitable for dev/test mode as other people on the network can connect to your
     * development machine.
     * <p>
     * As an exception, when running in Windows Subsystem for Linux (WSL), the HTTP host
     * defaults to 0.0.0.0 even in dev/test mode since using localhost makes the application
     * inaccessible.
     */
    @ConfigItem(generateDocumentation = false)
    public String host;

    /**
     * The HTTPS port
     */
    @ConfigItem(defaultValue = "8443", generateDocumentation = false)
    public int sslPort;

    /**
     * Static Resources.
     */
    public StaticResourcesConfig staticResources;

    @ConfigGroup
    @Deprecated(forRemoval = true, since = "3.19")
    public static class StaticResourcesConfig {

        /**
         * Set the index page when serving static resources.
         */
        @ConfigItem(defaultValue = "index.html", generateDocumentation = false)
        public String indexPage;
    }
}
