package io.quarkus.websockets.next.runtime.config;

import io.smallrye.config.WithDefault;

public interface TrafficLoggingConfig {

    /**
     * If set to true then binary/text messages received/sent are logged if the {@code DEBUG} level is enabled for the
     * logger {@code io.quarkus.websockets.next.traffic}.
     */
    @WithDefault("false")
    public boolean enabled();

    /**
     * The number of characters of a text message which will be logged if traffic logging is enabled. The payload of a
     * binary message is never logged.
     */
    @WithDefault("100")
    public int textPayloadLimit();
}