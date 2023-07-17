package io.quarkus.mailer.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "mailer", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class MailersBuildTimeConfig {

    /**
     * Caches data from attachment's Stream to a temporary file.
     * It tries to delete it after sending email.
     */
    @ConfigItem(defaultValue = "false")
    public boolean cacheAttachments;
}
