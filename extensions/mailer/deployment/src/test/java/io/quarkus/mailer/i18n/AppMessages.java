package io.quarkus.mailer.i18n;

import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;

@MessageBundle
public interface AppMessages {

    @Message("Hello {name}!")
    String hello_name(String name);

}
