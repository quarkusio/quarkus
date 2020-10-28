package io.quarkus.devtools.codestarts.quarkus;

import static java.util.Objects.requireNonNull;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.devtools.codestarts.CodestartProjectInputBuilder;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.extensions.Extensions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class QuarkusCodestartProjectInputBuilder extends CodestartProjectInputBuilder {
    public Collection<AppArtifactCoords> extensions = new ArrayList<>();
    boolean noExamples;
    boolean noDockerfiles;
    boolean noBuildToolWrapper;
    BuildTool buildTool = BuildTool.MAVEN;

    QuarkusCodestartProjectInputBuilder() {
        super();
    }

    public QuarkusCodestartProjectInputBuilder addExtensions(Collection<AppArtifactCoords> extensions) {
        this.extensions.addAll(extensions);
        super.addDependencies(extensions.stream().map(Extensions::toGAV).collect(Collectors.toList()));
        return this;
    }

    public QuarkusCodestartProjectInputBuilder addExtension(AppArtifactCoords extension) {
        return this.addExtensions(Collections.singletonList(extension));
    }

    public QuarkusCodestartProjectInputBuilder addExtension(AppArtifactKey extension) {
        return this.addExtension(Extensions.toCoords(extension, null));
    }

    @Override
    public QuarkusCodestartProjectInputBuilder addCodestarts(Collection<String> codestarts) {
        super.addCodestarts(codestarts);
        return this;
    }

    @Override
    public QuarkusCodestartProjectInputBuilder addCodestart(String codestart) {
        super.addCodestart(codestart);
        return this;
    }

    @Override
    public QuarkusCodestartProjectInputBuilder addData(Map<String, Object> data) {
        super.addData(data);
        return this;
    }

    @Override
    public QuarkusCodestartProjectInputBuilder putData(String key, Object value) {
        super.putData(key, value);
        return this;
    }

    @Override
    public QuarkusCodestartProjectInputBuilder messageWriter(MessageWriter messageWriter) {
        super.messageWriter(messageWriter);
        return this;
    }

    public QuarkusCodestartProjectInputBuilder noExamples() {
        return this.noExamples(true);
    }

    public QuarkusCodestartProjectInputBuilder noExamples(boolean noExamples) {
        this.noExamples = noExamples;
        return this;
    }

    public QuarkusCodestartProjectInputBuilder noDockerfiles() {
        return this.noDockerfiles(true);
    }

    public QuarkusCodestartProjectInputBuilder noDockerfiles(boolean noDockerfiles) {
        this.noDockerfiles = noDockerfiles;
        return this;
    }

    public QuarkusCodestartProjectInputBuilder noBuildToolWrapper() {
        return this.noBuildToolWrapper(true);
    }

    public QuarkusCodestartProjectInputBuilder noBuildToolWrapper(boolean noBuildToolWrapper) {
        this.noBuildToolWrapper = noBuildToolWrapper;
        return this;
    }

    public QuarkusCodestartProjectInputBuilder buildTool(BuildTool buildTool) {
        this.buildTool = requireNonNull(buildTool, "buildTool is required");
        return this;
    }

    public QuarkusCodestartProjectInput build() {
        return new QuarkusCodestartProjectInput(this);
    }

}
