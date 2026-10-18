package io.quarkus.resteasy.reactive.qute.deployment;

import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;

@MessageBundle
public interface GreetingMessages {

    @Message("Hello!")
    String hello();

    @Message("Hello {name}!")
    String hello_name(String name);

}
