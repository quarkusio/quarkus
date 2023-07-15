package io.quarkus.qute.deployment.i18n;

import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;

@MessageBundle(defaultKey = Message.UNDERSCORED_ELEMENT_NAME)
public interface CharlieMessages {

    @Message
    String helloAndMore();

    @Message(key = Message.HYPHENATED_ELEMENT_NAME)
    String helloAndLess();

}
