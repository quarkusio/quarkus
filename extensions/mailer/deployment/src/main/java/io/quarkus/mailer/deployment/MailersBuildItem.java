package io.quarkus.mailer.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

final class MailersBuildItem extends SimpleBuildItem {

    private final boolean hasDefaultMailer;

    private final Set<String> namedMailers;

    public MailersBuildItem(boolean hasDefaultMailer, Set<String> namedMailers) {
        this.hasDefaultMailer = hasDefaultMailer;
        this.namedMailers = namedMailers;
    }

    public boolean hasDefaultMailer() {
        return hasDefaultMailer;
    }

    public Set<String> getNamedMailers() {
        return namedMailers;
    }
}
