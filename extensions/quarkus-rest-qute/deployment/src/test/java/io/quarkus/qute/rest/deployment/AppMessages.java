package io.quarkus.qute.rest.deployment;

import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;

@MessageBundle
public interface AppMessages {

    @Message("Hello world!")
    String hello();

    @Message("Hello {name}!")
    String hello_name(String name);

}
