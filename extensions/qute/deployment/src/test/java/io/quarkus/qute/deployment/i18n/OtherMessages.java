package io.quarkus.qute.deployment.i18n;

import io.quarkus.qute.i18n.Localized;
import io.quarkus.qute.i18n.Message;

@Localized("cs")
public interface OtherMessages extends AppMessages {

    @Message("Ahoj {name}!")
    @Override
    String hello_name(String name);

}
