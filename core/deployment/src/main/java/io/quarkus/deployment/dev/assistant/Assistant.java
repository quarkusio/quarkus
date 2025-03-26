package io.quarkus.deployment.dev.assistant;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * This is the Assistant for Quarkus Dev Mode. The actual implementation will be provided by another extension (eg. Chappie)
 * If there is an extension that provide a concrete implementation of this, it will be available in the AssistantBuildItem,
 * so to use it, add Optional<AssistantBuildItem> assistantBuildItem in your BuildStep.
 */
public interface Assistant {
    // Exception
    default <T> CompletionStage<T> exception(String userMessage, String stacktrace, Path path) {
        return exception(Optional.empty(), userMessage, stacktrace, path);
    }

    public <T> CompletionStage<T> exception(Optional<String> systemMessage, String userMessage, String stacktrace,
            Path path);

    // Generic assist
    default <T> CompletionStage<T> assist(String userMessage) {
        return assist(Optional.empty(), userMessage, Map.of());
    }

    default <T> CompletionStage<T> assist(Optional<String> systemMessage, String userMessage) {
        return assist(systemMessage, userMessage, Map.of());
    }

    default <T> CompletionStage<T> assist(String userMessageTemplate, Map<String, String> variables) {
        return assist(Optional.empty(), userMessageTemplate, variables);
    }

    default <T> CompletionStage<T> assist(Optional<String> systemMessageTemplate, String userMessageTemplate,
            Map<String, String> variables) {
        return assist(systemMessageTemplate, userMessageTemplate, variables, List.of());
    }

    /**
     * Assist the developer with something
     *
     * @param <T> The response
     * @param systemMessageTemplate System wide context
     * @param userMessageTemplate User specific context
     * @param variables variables that can be used in the templates
     * @param paths Paths to workspace files (optional)
     * @return
     */
    public <T> CompletionStage<T> assist(Optional<String> systemMessageTemplate,
            String userMessageTemplate,
            Map<String, String> variables, List<Path> paths);
}
