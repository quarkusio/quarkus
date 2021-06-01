package io.quarkus.devtools.codestarts.quarkus;

import static java.util.Objects.requireNonNull;

import io.quarkus.devtools.codestarts.CodestartProjectInput;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.AppContent;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.maven.ArtifactCoords;
import java.util.Collection;
import java.util.Set;

public final class QuarkusCodestartProjectInput extends CodestartProjectInput {
    private final BuildTool buildTool;
    private final Collection<ArtifactCoords> extensions;
    private final Collection<ArtifactCoords> platforms;
    private final String example;
    private Set<AppContent> appContent;

    public QuarkusCodestartProjectInput(QuarkusCodestartProjectInputBuilder builder) {
        super(builder);
        this.extensions = builder.extensions;
        this.platforms = builder.platforms;
        this.example = builder.example;
        this.buildTool = requireNonNull(builder.buildTool, "buildTool is required");
        this.appContent = builder.appContent;
    }

    public static QuarkusCodestartProjectInputBuilder builder() {
        return new QuarkusCodestartProjectInputBuilder();
    }

    public Collection<ArtifactCoords> getExtensions() {
        return extensions;
    }

    public Collection<ArtifactCoords> getPlatforms() {
        return platforms;
    }

    public String getExample() {
        return example;
    }

    public Set<AppContent> getAppContent() {
        return appContent;
    }

    public BuildTool getBuildTool() {
        return buildTool;
    }
}
