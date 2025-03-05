package io.quarkus.mailer.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.mailer")
public interface MailersBuildTimeConfig {

    /**
     * Caches data from attachment's Stream to a temporary file.
     * It tries to delete it after sending email.
     */
    @WithDefault("false")
    boolean cacheAttachments();
}
