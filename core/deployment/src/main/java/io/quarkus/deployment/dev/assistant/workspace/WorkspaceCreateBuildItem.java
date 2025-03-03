package io.quarkus.deployment.dev.assistant.workspace;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * This is an action on a workspace item (file).
 * This generates new source using existing source as input. The generated source can be saved at a new location.
 */
public final class WorkspaceCreateBuildItem extends AbstractWorkspaceBuildItem {

    private final Function<Path, Path> storePathFunction;

    private WorkspaceCreateBuildItem(Builder builder) {
        super(builder.label, builder.systemMessage, builder.userMessage, builder.filter);
        this.storePathFunction = builder.storePathFunction;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String label;
        private Optional<String> systemMessage = Optional.empty();
        private String userMessage;
        private Function<Path, Path> storePathFunction;
        private Optional<Pattern> filter = Optional.empty();

        public Builder storePathFunction(Function<Path, Path> storePathFunction) {
            this.storePathFunction = storePathFunction;
            return this;
        }

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

        public WorkspaceCreateBuildItem build() {
            return new WorkspaceCreateBuildItem(this);
        }
    }

    public Function<Path, Path> getStorePathFunction() {
        return storePathFunction;
    }

    public Path resolveStorePath(Path sourcePath) {
        return storePathFunction.apply(sourcePath);
    }

    public String resolveStorePath(String sourcePath) {
        Path path = Paths.get(sourcePath);
        return resolveStorePath(path).toString();
    }
}
