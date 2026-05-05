package io.quarkus.email.authentication.runtime.internal;

import java.util.Set;

import io.quarkus.arc.DefaultBean;
import io.quarkus.email.authentication.EmailAuthenticationCodeSender;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.smallrye.mutiny.Uni;

@DefaultBean
final class DefaultEmailAuthenticationCodeSender implements EmailAuthenticationCodeSender {

    private final ReactiveMailer mailer;
    private final String emailSubject;
    private final String emailText;

    DefaultEmailAuthenticationCodeSender(ReactiveMailer mailer, EmailAuthenticationConfig config) {
        this.mailer = mailer;
        this.emailSubject = config.emailSubject();
        this.emailText = getEmailText(config);
    }

    @Override
    public Uni<Void> sendCode(char[] code, String emailAddress) {
        return mailer.send(createEmail(code, emailAddress));
    }

    private Mail createEmail(char[] code, String email) {
        return Mail.withText(email, emailSubject, emailText.formatted(String.valueOf(code)));
    }

    private static String getEmailText(EmailAuthenticationConfig config) {
        if (!config.emailText().contains("%s")) {
            throw new ConfigurationException("Email text must contain '%s' marking the position of email authentication code",
                    Set.of("quarkus.email-authentication.email-text"));
        }
        return config.emailText();
    }

}
