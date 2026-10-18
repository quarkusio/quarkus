package io.quarkus.qute.deployment.i18n;

import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;

@MessageBundle(value = "alpha", defaultKey = Message.HYPHENATED_ELEMENT_NAME)
public interface AlphaMessages {

    @Message("Hello alpha!")
    String helloAlpha();

    @Message(value = "Hello!", key = "hello_alpha")
    String hello();

    @Message("Hello {name} from alpha!")
    String helloWithParam(String name);

}
