package io.quarkus.mailer.runtime;

import java.util.Set;

public class MailerSupport {

    public final boolean hasDefaultMailer;

    public final Set<String> namedMailers;

    public MailerSupport(boolean hasDefaultMailer, Set<String> namedMailers) {
        this.hasDefaultMailer = hasDefaultMailer;
        this.namedMailers = namedMailers;
    }
}
