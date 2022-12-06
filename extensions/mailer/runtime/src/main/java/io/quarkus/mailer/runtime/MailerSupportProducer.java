package io.quarkus.mailer.runtime;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.runtime.LaunchMode;

@Singleton
public class MailerSupportProducer {

    @Produces
    @Singleton
    public MailerSupport mailSupportProducer(MailConfig config, LaunchMode launchMode) {
        return new MailerSupport(
                config.from.orElse(null),
                config.bounceAddress.orElse(null),
                config.mock.orElse(launchMode.isDevOrTest()));
    }
}
