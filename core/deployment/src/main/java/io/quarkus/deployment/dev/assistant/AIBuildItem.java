package io.quarkus.deployment.dev.assistant;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * If available, a handle on the AI Client
 *
 * This is intended for use in dev mode to allow Quarkus to help the developer.
 */
public final class AIBuildItem extends SimpleBuildItem {

    private final AIClient aiClient;

    public AIBuildItem(AIClient aiClient) {
        this.aiClient = aiClient;
    }

    public AIClient getAIClient() {
        return aiClient;
    }
}
