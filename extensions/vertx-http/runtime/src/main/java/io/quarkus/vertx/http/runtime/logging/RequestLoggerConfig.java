package io.quarkus.vertx.http.runtime.logging;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class RequestLoggerConfig {

    /**
     * Enable logging for all HTTP requests.
     */
    @ConfigItem(defaultValue = "false")
    public boolean enabled;

    /**
     * The HTTP Request log format.
     * The possible values are: LONG, DEFAULT, SHORT and TINY.
     */
    @ConfigItem(defaultValue = "DEFAULT")
    public RequestLoggerFormat format;

}
