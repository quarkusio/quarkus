package io.quarkus.arc.test.contexts.conversation;

import jakarta.enterprise.context.ConversationScoped;

@ConversationScoped
public class ConversationBean {

    void ping() {
    }
}
