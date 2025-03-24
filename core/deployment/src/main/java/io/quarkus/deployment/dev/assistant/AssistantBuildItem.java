package io.quarkus.deployment.dev.assistant;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * If available, a handle on the Assistant
 *
 * This is intended for use in dev mode to enable AI-enhanced development
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
