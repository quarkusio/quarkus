package io.quarkus.devtools.codestarts;

import static java.util.Objects.requireNonNull;

import io.quarkus.bootstrap.model.AppArtifactKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CodestartInputBuilder {
    private CodestartResourceLoader resourceLoader;
    private Map<AppArtifactKey, String> extensionCodestartMapping;
    private Collection<AppArtifactKey> extensions = new ArrayList<>();
    private Collection<String> codestarts = new ArrayList<>();
    private boolean includeExamples = false;
    private Map<String, Object> data = new HashMap<>();

    CodestartInputBuilder(CodestartResourceLoader resourceLoader, Map<AppArtifactKey, String> extensionCodestartMapping) {
        this.resourceLoader = resourceLoader;
        this.extensionCodestartMapping = requireNonNull(extensionCodestartMapping, "extensionCodestartMapping is required");
    }

    public CodestartInputBuilder addExtensions(Collection<AppArtifactKey> extensions) {
        this.extensions.addAll(extensions);
        final Set<String> codestarts = extensions.stream()
                .filter(extensionCodestartMapping::containsKey)
                .map(extensionCodestartMapping::get)
                .collect(Collectors.toSet());
        this.addCodestarts(codestarts);
        return this;
    }

    public CodestartInputBuilder addExtension(AppArtifactKey extension) {
        return this.addExtensions(Collections.singletonList(extension));
    }

    public CodestartInputBuilder addCodestarts(Collection<String> codestarts) {
        this.codestarts.addAll(codestarts);
        return this;
    }

    public CodestartInputBuilder includeExamples() {
        return includeExamples(true);
    }

    public CodestartInputBuilder includeExamples(boolean includeExamples) {
        this.includeExamples = includeExamples;
        return this;
    }

    public CodestartInputBuilder addData(Map<String, Object> data) {
        this.data.putAll(data);
        return this;
    }

    public CodestartInputBuilder addCodestart(String name) {
        this.codestarts.add(name);
        return this;
    }

    public CodestartInputBuilder putData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    public CodestartInput build() {
        return new CodestartInput(resourceLoader, extensions, codestarts, includeExamples, NestedMaps.unflatten(data));
    }
}
