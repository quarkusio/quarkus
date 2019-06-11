package io.quarkus.mailer.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.vertx.ext.mail.MailClient;

public final class MailerBuildItem extends SimpleBuildItem {

    private final RuntimeValue<MailClient> client;

    public MailerBuildItem(RuntimeValue<MailClient> client) {
        this.client = client;
    }

    public RuntimeValue<MailClient> getClient() {
        return client;
    }

}
