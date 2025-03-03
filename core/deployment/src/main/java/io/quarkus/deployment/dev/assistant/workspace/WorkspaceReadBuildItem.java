package io.quarkus.deployment.dev.assistant.workspace;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * This is an action on a workspace item (file).
 * This interprets content. The output will be in markdown.
 */
public final class WorkspaceReadBuildItem extends AbstractWorkspaceBuildItem {

    private WorkspaceReadBuildItem(Builder builder) {
        super(builder.label, builder.systemMessage, builder.userMessage, builder.filter);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String label;
        private Optional<String> systemMessage = Optional.empty();
        private String userMessage;
        private Optional<Pattern> filter = Optional.empty();

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder systemMessage(String systemMessage) {
            this.systemMessage = Optional.of(systemMessage);
            return this;
        }

        public Builder userMessage(String userMessage) {
            this.userMessage = userMessage;
            return this;
        }

        public Builder filter(Pattern filter) {
            this.filter = Optional.of(filter);
            return this;
        }

        public WorkspaceReadBuildItem build() {
            return new WorkspaceReadBuildItem(this);
        }
    }
}
