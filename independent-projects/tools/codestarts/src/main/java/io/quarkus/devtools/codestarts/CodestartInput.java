package io.quarkus.devtools.codestarts;

import static java.util.Objects.requireNonNull;

import io.quarkus.bootstrap.model.AppArtifactKey;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class CodestartInput {
    private final CodestartResourceLoader resourceLoader;
    private final Collection<AppArtifactKey> extensions;
    private final boolean includeExamples;
    private final Map<String, Object> data;
    private final Collection<String> codestarts;

    CodestartInput(CodestartResourceLoader resourceLoader, Collection<AppArtifactKey> extensions,
            Collection<String> codestarts, boolean includeExamples, Map<String, Object> data) {
        this.resourceLoader = requireNonNull(resourceLoader, "resourceLoader is required");
        this.extensions = requireNonNull(extensions, "extensions is required");
        this.codestarts = requireNonNull(codestarts, "codestarts is required");
        this.includeExamples = requireNonNull(includeExamples, "includeExamples is required");
        this.data = requireNonNull(data, "data is required");
    }

    public static CodestartInputBuilder builder(CodestartResourceLoader resourceLoader) {
        return new CodestartInputBuilder(resourceLoader, Collections.emptyMap());
    }

    public static CodestartInputBuilder builder(CodestartResourceLoader resourceLoader,
            Map<AppArtifactKey, String> extensionCodestartMapping) {
        return new CodestartInputBuilder(resourceLoader, extensionCodestartMapping);
    }

    public CodestartResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    public Collection<String> getCodestarts() {
        return codestarts;
    }

    public Collection<AppArtifactKey> getExtensions() {
        return extensions;
    }

    public boolean includeExamples() {
        return includeExamples;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
