package io.quarkus.resteasy.reactive.server.runtime;

import java.nio.charset.Charset;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.rest")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ResteasyReactiveServerRuntimeConfig {

    /**
     * Input part configuration.
     */
    MultipartConfigGroup multipart();

    interface MultipartConfigGroup {

        /**
         * Input part configuration.
         */
        InputPartConfigGroup inputPart();
    }

    interface InputPartConfigGroup {

        /**
         * Default charset.
         */
        @WithDefault("UTF-8")
        Charset defaultCharset();
    }
}
