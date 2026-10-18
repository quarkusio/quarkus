package io.quarkus.resteasy.multipart.runtime;

import java.nio.charset.Charset;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.resteasy.multipart")
public interface ResteasyMultipartRuntimeConfig {

    /**
     * Input part configuration.
     */
    InputPartConfigGroup inputPart();

    @ConfigGroup
    interface InputPartConfigGroup {

        /**
         * Default charset.
         * <p>
         * Note that the default value is UTF-8 which is different from RESTEasy's default value US-ASCII.
         */
        @WithDefault("UTF-8")
        Charset defaultCharset();

        /**
         * The default content-type.
         */
        @WithDefault("text/plain")
        String defaultContentType();
    }
}
