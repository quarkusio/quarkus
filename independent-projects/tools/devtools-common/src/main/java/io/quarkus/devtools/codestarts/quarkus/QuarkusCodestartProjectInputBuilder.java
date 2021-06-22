package io.quarkus.devtools.codestarts.quarkus;

import io.quarkus.devtools.codestarts.CodestartProjectInputBuilder;
import io.quarkus.devtools.codestarts.DataKey;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.AppContent;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.extensions.Extensions;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.maven.ArtifactKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class QuarkusCodestartProjectInputBuilder extends CodestartProjectInputBuilder {
    private static final List<AppContent> FULL_CONTENT = Arrays.asList(AppContent.values());

    Collection<ArtifactCoords> extensions = new ArrayList<>();
    Collection<ArtifactCoords> platforms = new ArrayList<>();
    Set<AppContent> appContent = new HashSet<>(FULL_CONTENT);
    String example;
    BuildTool buildTool = BuildTool.MAVEN;

    QuarkusCodestartProjectInputBuilder() {
        super();
    }

    public QuarkusCodestartProjectInputBuilder addExtensions(Collection<ArtifactCoords> extensions) {
        this.extensions.addAll(extensions);
        super.addDependencies(extensions.stream().map(Extensions::toGAV).collect(Collectors.toList()));
        return this;
    }

    public QuarkusCodestartProjectInputBuilder addExtension(ArtifactCoords extension) {
        return this.addExtensions(Collections.singletonList(extension));
    }

    public QuarkusCodestartProjectInputBuilder addExtension(ArtifactKey extension) {
        return this.addExtension(Extensions.toCoords(extension, null));
    }

    public QuarkusCodestartProjectInputBuilder addPlatforms(Collection<ArtifactCoords> boms) {
        this.platforms.addAll(boms);
        super.addBoms(boms.stream().map(Extensions::toGAV).collect(Collectors.toList()));
        return this;
    }

    public QuarkusCodestartProjectInputBuilder example(String example) {
        this.example = example;
        return this;
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
    public QuarkusCodestartProjectInputBuilder addBoms(Collection<String> boms) {
        super.addBoms(boms);
        return this;
    }

    @Override
    public QuarkusCodestartProjectInputBuilder putData(String key, Object value) {
        super.putData(key, value);
        return this;
    }

    @Override
    public QuarkusCodestartProjectInputBuilder putData(DataKey key, Object value) {
        super.putData(key, value);
        return this;
    }

    @Override
    public QuarkusCodestartProjectInputBuilder messageWriter(MessageWriter messageWriter) {
        super.messageWriter(messageWriter);
        return this;
    }

    public QuarkusCodestartProjectInputBuilder noCode() {
        return this.noCode(true);
    }

    public QuarkusCodestartProjectInputBuilder noCode(boolean noCode) {
        if (noCode) {
            appContent.remove(AppContent.CODE);
        } else {
            appContent.add(AppContent.CODE);
        }
        return this;
    }

    public QuarkusCodestartProjectInputBuilder noDockerfiles() {
        return this.noDockerfiles(true);
    }

    public QuarkusCodestartProjectInputBuilder noDockerfiles(boolean noDockerfiles) {
        if (noDockerfiles) {
            appContent.remove(AppContent.DOCKERFILES);
        } else {
            appContent.add(AppContent.DOCKERFILES);
        }
        return this;
    }

    public QuarkusCodestartProjectInputBuilder noBuildToolWrapper() {
        return this.noBuildToolWrapper(true);
    }

    public QuarkusCodestartProjectInputBuilder noBuildToolWrapper(boolean noBuildToolWrapper) {
        if (noBuildToolWrapper) {
            appContent.remove(AppContent.BUILD_TOOL_WRAPPER);
        } else {
            appContent.add(AppContent.BUILD_TOOL_WRAPPER);
        }
        return this;
    }

    public QuarkusCodestartProjectInputBuilder buildTool(BuildTool buildTool) {
        if (buildTool == null) {
            return this;
        }
        this.buildTool = buildTool;
        return this;
    }

    public QuarkusCodestartProjectInput build() {
        return new QuarkusCodestartProjectInput(this);
    }

}
