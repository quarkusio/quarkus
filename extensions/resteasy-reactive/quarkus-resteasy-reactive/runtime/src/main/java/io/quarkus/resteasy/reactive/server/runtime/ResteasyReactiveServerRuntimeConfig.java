package io.quarkus.resteasy.reactive.server.runtime;

import java.nio.charset.Charset;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "resteasy-reactive", phase = ConfigPhase.RUN_TIME)
public class ResteasyReactiveServerRuntimeConfig {

    /**
     * Input part configuration.
     */
    @ConfigItem
    public MultipartConfigGroup multipart;

    @ConfigGroup
    public static class MultipartConfigGroup {

        /**
         * Input part configuration.
         */
        @ConfigItem
        public InputPartConfigGroup inputPart;
    }

    @ConfigGroup
    public static class InputPartConfigGroup {

        /**
         * Default charset.
         */
        @ConfigItem(defaultValue = "UTF-8")
        public Charset defaultCharset;
    }
}
