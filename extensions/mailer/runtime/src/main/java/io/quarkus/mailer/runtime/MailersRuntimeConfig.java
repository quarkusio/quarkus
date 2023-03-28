package io.quarkus.mailer.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "mailer", phase = ConfigPhase.RUN_TIME)
public class MailersRuntimeConfig {

    /**
     * The default mailer.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public MailerRuntimeConfig defaultMailer;

    /**
     * Additional named mailers.
     */
    @ConfigDocSection
    @ConfigDocMapKey("mailer-name")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, MailerRuntimeConfig> namedMailers;
}
