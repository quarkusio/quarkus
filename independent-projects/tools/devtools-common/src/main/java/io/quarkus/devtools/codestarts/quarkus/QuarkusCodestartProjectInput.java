package io.quarkus.devtools.codestarts.quarkus;

import static java.util.Objects.requireNonNull;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.devtools.codestarts.CodestartProjectInput;
import io.quarkus.devtools.project.BuildTool;
import java.util.Collection;
import java.util.Set;

public final class QuarkusCodestartProjectInput extends CodestartProjectInput {
    private final BuildTool buildTool;
    private final Collection<AppArtifactCoords> extensions;
    private final Set<String> overrideExamples;
    private final boolean noExamples;
    private final boolean noDockerfiles;
    private final boolean noBuildToolWrapper;

    public QuarkusCodestartProjectInput(QuarkusCodestartProjectInputBuilder builder) {
        super(builder);
        this.extensions = builder.extensions;
        this.overrideExamples = builder.overrideExamples;
        this.buildTool = requireNonNull(builder.buildTool, "buildTool is required");
        this.noExamples = builder.noExamples;
        this.noDockerfiles = builder.noDockerfiles;
        this.noBuildToolWrapper = builder.noBuildToolWrapper;
    }

    public static QuarkusCodestartProjectInputBuilder builder() {
        return new QuarkusCodestartProjectInputBuilder();
    }

    public Collection<AppArtifactCoords> getExtensions() {
        return extensions;
    }

    public Set<String> getOverrideExamples() {
        return overrideExamples;
    }

    public boolean noExamples() {
        return noExamples;
    }

    public boolean noDockerfiles() {
        return noDockerfiles;
    }

    public boolean noBuildToolWrapper() {
        return noBuildToolWrapper;
    }

    public BuildTool getBuildTool() {
        return buildTool;
    }
}
