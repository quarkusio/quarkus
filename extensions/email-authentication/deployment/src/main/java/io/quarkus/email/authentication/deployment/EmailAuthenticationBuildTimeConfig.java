package io.quarkus.email.authentication.deployment;

import io.quarkus.mailer.runtime.Mailers;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Email authentication configuration.
 */
@ConfigMapping(prefix = "quarkus.email-authentication")
@ConfigRoot
interface EmailAuthenticationBuildTimeConfig {

    /**
     * If the email authentication is enabled.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Name of the mailer used to send email authentication codes.
     * This configuration property is used by the default {@link io.quarkus.email.authentication.EmailAuthenticationCodeSender}
     * implementation.
     */
    @WithDefault(Mailers.DEFAULT_MAILER_NAME)
    String mailerName();

}
