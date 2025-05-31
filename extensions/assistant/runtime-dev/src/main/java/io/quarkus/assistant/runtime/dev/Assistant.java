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
     * @return true id there is an implementation that is configured and connected
     */
    public boolean isAvailable();

    /**
     * Explain and suggest a fix for an exception in the user code
     *
     * @param <T>
     * @param systemMessage System wide context
     * @param userMessage User specific context
     * @param stacktrace The exception stacktrace
     * @param path The path to the effective class causing this exception
     * @return
     */
    <T> CompletionStage<T> exception(Optional<String> systemMessage, String userMessage, String stacktrace,
            Path path);

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
            Map<String, String> variables, List<Path> paths);

    default ExceptionBuilder exceptionBuilder() {
        return new ExceptionBuilder(this);
    }

    default AssistBuilder assistBuilder() {
        return new AssistBuilder(this);
    }

    // Builder class
    class AssistBuilder {
        private final Assistant assistant;
        private Optional<String> systemMessage = Optional.empty();
        private String userMessage;
        private final Map<String, String> variables = new LinkedHashMap<>();
        private final List<Path> paths = new ArrayList<>();

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

        @SuppressWarnings("unchecked")
        public <T> CompletionStage<T> assist() {
            if (userMessage == null || userMessage.isBlank()) {
                throw new IllegalStateException("User message is required");
            }
            return (CompletionStage<T>) assistant.assist(systemMessage, userMessage, variables, paths);
        }
    }

    class ExceptionBuilder {
        private final Assistant assistant;

        private Optional<String> systemMessage = Optional.empty();
        private String userMessage;
        private String stacktrace;
        private Path path;

        ExceptionBuilder(Assistant assistant) {
            this.assistant = assistant;
        }

        public ExceptionBuilder systemMessage(String systemMessage) {
            this.systemMessage = Optional.ofNullable(systemMessage);
            return this;
        }

        public ExceptionBuilder userMessage(String userMessage) {
            this.userMessage = userMessage;
            return this;
        }

        public ExceptionBuilder stacktrace(String stacktrace) {
            this.stacktrace = stacktrace;
            return this;
        }

        public ExceptionBuilder path(Path path) {
            this.path = path;
            return this;
        }

        @SuppressWarnings("unchecked")
        public <T> CompletionStage<T> explain() {
            if (userMessage == null || stacktrace == null || path == null) {
                throw new IllegalStateException("userMessage, stacktrace, and path must be provided");
            }
            return (CompletionStage<T>) assistant.exception(systemMessage, userMessage, stacktrace, path);
        }
    }

}
