package io.quarkus.resteasy.multipart.runtime;

import java.nio.charset.Charset;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "resteasy.multipart", phase = ConfigPhase.RUN_TIME)
public class ResteasyMultipartRuntimeConfig {

    /**
     * Input part configuration.
     */
    @ConfigItem
    public InputPartConfigGroup inputPart;

    @ConfigGroup
    public static class InputPartConfigGroup {

        /**
         * Default charset.
         * <p>
         * Note that the default value is UTF-8 which is different from RESTEasy's default value US-ASCII.
         */
        @ConfigItem(defaultValue = "UTF-8")
        public Charset defaultCharset;

        /**
         * The default content-type.
         */
        @ConfigItem(defaultValue = "text/plain")
        public String defaultContentType;
    }
}
