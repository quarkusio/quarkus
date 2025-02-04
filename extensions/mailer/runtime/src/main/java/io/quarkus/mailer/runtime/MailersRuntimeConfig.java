package io.quarkus.mailer.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.mailer")
public interface MailersRuntimeConfig {

    /**
     * Additional named mailers.
     */
    @ConfigDocSection
    @ConfigDocMapKey("mailer-name")
    @WithParentName
    @WithDefaults
    @WithUnnamedKey(Mailers.DEFAULT_MAILER_NAME)
    Map<String, MailerRuntimeConfig> mailers();
}
