package io.quarkus.qute.deployment.i18n.acme_trans;

import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;

@MessageBundle
public interface TransMessages {

    @Message
    String hello();

}
