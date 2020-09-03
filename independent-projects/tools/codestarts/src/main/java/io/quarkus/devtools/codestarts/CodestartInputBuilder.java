package io.quarkus.devtools.codestarts;

import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.devtools.messagewriter.MessageWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CodestartInputBuilder {
    CodestartResourceLoader resourceLoader;
    Map<AppArtifactKey, String> extensionCodestartMapping;
    Collection<AppArtifactKey> dependencies = new ArrayList<>();
    Collection<String> codestarts = new ArrayList<>();
    Map<String, Object> data = new HashMap<>();
    MessageWriter messageWriter = MessageWriter.DEFAULT;

    CodestartInputBuilder(CodestartResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public CodestartInputBuilder addDependencies(Collection<AppArtifactKey> dependencies) {
        this.dependencies.addAll(dependencies);
        return this;
    }

    public CodestartInputBuilder addDependency(AppArtifactKey dependency) {
        return this.addDependencies(Collections.singletonList(dependency));
    }

    public CodestartInputBuilder addCodestarts(Collection<String> codestarts) {
        this.codestarts.addAll(codestarts);
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

    public CodestartInputBuilder messageWriter(MessageWriter messageWriter) {
        this.messageWriter = messageWriter;
        return this;
    }

    public CodestartInput build() {
        return new CodestartInput(this);
    }
}
