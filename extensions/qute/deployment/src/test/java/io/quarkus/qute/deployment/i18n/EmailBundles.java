package io.quarkus.qute.deployment.i18n;

import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;

public class EmailBundles {
    @MessageBundle
    interface started {
        @Message
        String started(String id, String filename);

        @Message
        String documentAccessUrl(String url);

        @Message
        String nextNotification();

        @Message
        String signingProcessStart(String id, String filename);

        @Message
        String subject(String customer, String filename);

        @Message
        String signForValidation();
    }

    @MessageBundle
    interface startedValidator {
        @Message
        String started(String id, String filename);

        @Message
        String turnEmailWillBeSent();

        @Message
        String youMayAlreadyAccessDocument();

        @Message
        String subject(String customer, String filename);

        @Message
        String signForValidation();
    }
}
