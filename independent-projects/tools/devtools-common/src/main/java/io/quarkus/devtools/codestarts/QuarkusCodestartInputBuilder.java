package io.quarkus.devtools.codestarts;

import static io.quarkus.devtools.codestarts.QuarkusCodestarts.resourceLoader;
import static java.util.Objects.requireNonNull;

import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.dependencies.Extension;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class QuarkusCodestartInputBuilder {
    final CodestartInputBuilder inputBuilder;
    final Map<AppArtifactKey, String> extensionCodestartMapping;
    boolean noExamples;
    boolean noDockerfiles;
    boolean noBuildToolWrapper;
    private BuildTool buildTool = BuildTool.MAVEN;

    QuarkusCodestartInputBuilder(QuarkusPlatformDescriptor platformDescr) {
        this.inputBuilder = CodestartInput.builder(resourceLoader(platformDescr));
        this.extensionCodestartMapping = buildCodestartMapping(platformDescr.getExtensions());
    }

    public QuarkusCodestartInputBuilder addExtensions(Collection<AppArtifactKey> extensions) {
        final Set<String> codestarts = extensions.stream()
                .filter(extensionCodestartMapping::containsKey)
                .map(extensionCodestartMapping::get)
                .collect(Collectors.toSet());
        this.addCodestarts(codestarts);
        this.inputBuilder.addDependencies(extensions);
        return this;
    }

    public QuarkusCodestartInputBuilder addExtension(AppArtifactKey extension) {
        return this.addExtensions(Collections.singletonList(extension));
    }

    public QuarkusCodestartInputBuilder addCodestarts(Collection<String> codestarts) {
        this.inputBuilder.addCodestarts(codestarts);
        return this;
    }

    public QuarkusCodestartInputBuilder addCodestart(String codestart) {
        this.inputBuilder.addCodestart(codestart);
        return this;
    }

    public QuarkusCodestartInputBuilder addData(Map<String, Object> data) {
        this.inputBuilder.addData(data);
        return this;
    }

    public QuarkusCodestartInputBuilder putData(String key, Object value) {
        this.inputBuilder.putData(key, value);
        return this;
    }

    public QuarkusCodestartInputBuilder noExamples() {
        return this.noExamples(true);
    }

    public QuarkusCodestartInputBuilder noExamples(boolean noExamples) {
        this.noExamples = noExamples;
        return this;
    }

    public QuarkusCodestartInputBuilder noDockerfiles() {
        return this.noDockerfiles(true);
    }

    public QuarkusCodestartInputBuilder noDockerfiles(boolean noDockerfiles) {
        this.noDockerfiles = noDockerfiles;
        return this;
    }

    public QuarkusCodestartInputBuilder noBuildToolWrapper() {
        return this.noBuildToolWrapper(true);
    }

    public QuarkusCodestartInputBuilder noBuildToolWrapper(boolean noBuildToolWrapper) {
        this.noBuildToolWrapper = noBuildToolWrapper;
        return this;
    }

    public QuarkusCodestartInputBuilder buildTool(BuildTool buildTool) {
        this.buildTool = requireNonNull(buildTool, "buildTool is required");
        ;
        return this;
    }

    public QuarkusCodestartInput build() {
        this.inputBuilder.addCodestarts(getToolingCodestarts());
        return new QuarkusCodestartInput(this);
    }

    private List<String> getToolingCodestarts() {
        final List<String> codestarts = new ArrayList<>();
        codestarts.add(buildTool.getKey());
        if (!noBuildToolWrapper) {
            switch (buildTool) {
                case GRADLE:
                case GRADLE_KOTLIN_DSL:
                    codestarts.add(QuarkusCodestarts.Tooling.GRADLE_WRAPPER.getKey());
                    break;
                case MAVEN:
                    codestarts.add(QuarkusCodestarts.Tooling.MAVEN_WRAPPER.getKey());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported build tool wrapper: " + buildTool);
            }
        }
        if (!noDockerfiles) {
            codestarts.add(QuarkusCodestarts.Tooling.DOCKERFILES.getKey());
        }
        return codestarts;
    }

    private static Map<AppArtifactKey, String> buildCodestartMapping(Collection<Extension> extensions) {
        return extensions.stream()
                .filter(e -> e.getCodestart() != null)
                .collect(Collectors.toMap(e -> new AppArtifactKey(e.getGroupId(), e.getArtifactId(), e.getClassifier(),
                        e.getType() == null ? "jar" : e.getType()), Extension::getCodestart));
    }

}
