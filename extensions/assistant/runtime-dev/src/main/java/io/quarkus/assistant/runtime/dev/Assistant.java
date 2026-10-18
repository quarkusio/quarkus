package io.quarkus.assistant.runtime.dev;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * This is the Assistant for Quarkus Dev Mode. The actual implementation will be provided by another extension (eg. Chappie)
 */
public interface Assistant {

    /**
     * Check if the assistant is available
     *
     * @return true if there is an implementation that is configured and connected
     */
    public boolean isAvailable();

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
    <T> CompletionStage<T> assist(Optional<String> systemMessageTemplate,
            String userMessageTemplate,
            Map<String, String> variables, List<Path> paths, Class<?> responseType);

    default AssistBuilder assistBuilder() {
        return new AssistBuilder(this);
    }

    /**
     * Return the memory Id from the last assistance. Could be null
     *
     * @return
     */
    public String getMemoryId();

    /**
     * Return the path to the assistant implementation chat screen
     *
     * @return
     */
    public String getChatPath();

    /**
     * Get the link to the chat screen given the current active chat
     *
     * @return
     */
    default String getLinkToChatScreen() {
        return getChatPath() + "?memoryId=" + getMemoryId();
    }

    // Builder class
    class AssistBuilder {
        private final Assistant assistant;
        private Optional<String> systemMessage = Optional.empty();
        private String userMessage;
        private final Map<String, String> variables = new LinkedHashMap<>();
        private final List<Path> paths = new ArrayList<>();
        private Class<?> responseType = null;

        AssistBuilder(Assistant assistant) {
            this.assistant = assistant;
        }

        public AssistBuilder systemMessage(String systemMessage) {
            this.systemMessage = Optional.ofNullable(systemMessage);
            return this;
        }

        public AssistBuilder userMessage(String userMessage) {
            this.userMessage = userMessage;
            return this;
        }

        public AssistBuilder variables(Map<String, String> variables) {
            if (variables != null) {
                this.variables.putAll(variables);
            }
            return this;
        }

        public AssistBuilder addVariable(String key, String value) {
            if (key != null && value != null) {
                this.variables.put(key, value);
            }
            return this;
        }

        public AssistBuilder paths(List<Path> paths) {
            if (paths != null) {
                this.paths.addAll(paths);
            }
            return this;
        }

        public AssistBuilder addPath(Path path) {
            if (path != null) {
                this.paths.add(path);
            }
            return this;
        }

        public AssistBuilder responseType(Class<?> responseType) {
            if (responseType != null) {
                this.responseType = responseType;
            }
            return this;
        }

        @SuppressWarnings("unchecked")
        public <T> CompletionStage<T> assist() {
            if (null == userMessage || userMessage.isBlank()) {
                throw new IllegalStateException("User message is required");
            }
            return (CompletionStage<T>) assistant.assist(systemMessage, userMessage, variables, paths, responseType);
        }
    }

}
