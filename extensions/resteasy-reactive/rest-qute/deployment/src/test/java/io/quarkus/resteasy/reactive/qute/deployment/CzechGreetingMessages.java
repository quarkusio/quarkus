package io.quarkus.resteasy.reactive.qute.deployment;

import io.quarkus.qute.i18n.Localized;
import io.quarkus.qute.i18n.Message;

@Localized("cs")
public interface CzechGreetingMessages extends GreetingMessages {

    @Override
    @Message("Ahoj!")
    String hello();

    @Override
    @Message("Ahoj {name}!")
    String hello_name(String name);

}
