package io.quarkus.devui.runtime;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.assistant.runtime.dev.Assistant;

@ApplicationScoped
public class AssistantJsonRPCService {

    @Inject
    Optional<Assistant> assistant;

    public String getLinkToChat() {
        if (assistant.isPresent()) {
            return assistant.get().getLinkToChatScreen();
        }
        return null;
    }
}
