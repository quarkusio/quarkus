package io.quarkus.assistant.deployment.spi;

import io.quarkus.assistant.runtime.dev.Assistant;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * If available, a handle on the Assistant
 *
 * This is intended for use in dev mode to enable AI-enhanced development
 * Extensions should NOT produce this. This will be produced by Chappie.
 *
 * Consume this to get a handle in the Assistant. Always use Optional<AssistantBuildItem> as
 * this is not guaranteed to be available
 *
 */
public final class AssistantBuildItem extends SimpleBuildItem {
    private final Assistant assistant;

    public AssistantBuildItem(Assistant assistant) {
        this.assistant = assistant;
    }

    public Assistant getAssistant() {
        return assistant;
    }
}
