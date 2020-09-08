package io.quarkus.devtools.codestarts;

import static java.util.Objects.requireNonNull;

import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.devtools.messagewriter.MessageWriter;
import java.util.Collection;
import java.util.Map;

public class CodestartInput {
    private final CodestartResourceLoader resourceLoader;
    private final Collection<AppArtifactKey> dependencies;
    private final Map<String, Object> data;
    private final Collection<String> codestarts;
    private final MessageWriter messageWriter;

    CodestartInput(final CodestartInputBuilder builder) {
        this.resourceLoader = requireNonNull(builder.resourceLoader, "resourceLoader is required");
        this.dependencies = requireNonNull(builder.dependencies, "dependencies is required");
        this.codestarts = requireNonNull(builder.codestarts, "codestarts is required");
        this.data = NestedMaps.unflatten(requireNonNull(builder.data, "data is required"));
        this.messageWriter = requireNonNull(builder.messageWriter, "messageWriter is required");
    }

    public static CodestartInputBuilder builder(CodestartResourceLoader resourceLoader) {
        return new CodestartInputBuilder(resourceLoader);
    }

    public MessageWriter log() {
        return messageWriter;
    }

    public CodestartResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    public Collection<String> getCodestarts() {
        return codestarts;
    }

    public Collection<AppArtifactKey> getDependencies() {
        return dependencies;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
