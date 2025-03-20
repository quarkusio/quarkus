package io.quarkus.deployment.dev.assistant.workspace;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * This is an action on a workspace item (file).
 * This is used to manipulate existing source in some way using AI. The manipulated source can be used to override
 * the provided source.
 */
public final class WorkspaceUpdateBuildItem extends AbstractWorkspaceBuildItem {

    private WorkspaceUpdateBuildItem(Builder builder) {
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

        public WorkspaceUpdateBuildItem build() {
            return new WorkspaceUpdateBuildItem(this);
        }
    }
}