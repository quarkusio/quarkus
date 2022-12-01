package io.quarkus.devtools.codestarts.jbang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import io.quarkus.devtools.codestarts.CodestartProjectInputBuilder;
import io.quarkus.devtools.codestarts.DataKey;
import io.quarkus.devtools.codestarts.utils.NestedMaps;
import io.quarkus.devtools.commands.CreateProjectHelper;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.extensions.Extensions;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;

public class QuarkusJBangCodestartProjectInputBuilder extends CodestartProjectInputBuilder {
    public Collection<ArtifactCoords> extensions = new ArrayList<>();

    public QuarkusJBangCodestartProjectInputBuilder setNoJBangWrapper(boolean noJBangWrapper) {
        this.noJBangWrapper = noJBangWrapper;
        return this;
    }

    public boolean noJBangWrapper;

    QuarkusJBangCodestartProjectInputBuilder() {
        super();
    }

    public QuarkusJBangCodestartProjectInputBuilder addExtensions(Collection<ArtifactCoords> extensions) {
        this.extensions.addAll(extensions);
        super.addDependencies(extensions.stream().map(Extensions::toGAV).collect(Collectors.toList()));
        return this;
    }

    public QuarkusJBangCodestartProjectInputBuilder addExtension(ArtifactCoords extension) {
        return this.addExtensions(Collections.singletonList(extension));
    }

    public QuarkusJBangCodestartProjectInputBuilder addExtension(ArtifactKey extension) {
        return this.addExtension(Extensions.toCoords(extension, null));
    }

    @Override
    public QuarkusJBangCodestartProjectInputBuilder addCodestarts(Collection<String> codestarts) {
        super.addCodestarts(codestarts);
        return this;
    }

    @Override
    public QuarkusJBangCodestartProjectInputBuilder addCodestart(String codestart) {
        super.addCodestart(codestart);
        return this;
    }

    @Override
    public QuarkusJBangCodestartProjectInputBuilder addData(Map<String, Object> data) {
        super.addData(data);
        return this;
    }

    @Override
    public QuarkusJBangCodestartProjectInputBuilder putData(String key, Object value) {
        super.putData(key, value);
        return this;
    }

    @Override
    public QuarkusJBangCodestartProjectInputBuilder putData(DataKey key, Object value) {
        super.putData(key, value);
        return this;
    }

    @Override
    public QuarkusJBangCodestartProjectInputBuilder messageWriter(MessageWriter messageWriter) {
        super.messageWriter(messageWriter);
        return this;
    }

    public boolean noJBangWrapper() {
        return noJBangWrapper;
    }

    public QuarkusJBangCodestartProjectInput build() {
        if (!this.containsData("java")) {
            this.addData(NestedMaps
                    .unflatten(Map.of("java.version", String.valueOf(CreateProjectHelper.determineBestJavaLtsVersion()))));
        }
        return new QuarkusJBangCodestartProjectInput(this);
    }

}
