package io.quarkus.devtools.codestarts.jbang;

import io.quarkus.devtools.codestarts.CodestartProjectInput;
import io.quarkus.maven.ArtifactCoords;
import java.util.Collection;

public final class QuarkusJBangCodestartProjectInput extends CodestartProjectInput {
    private final Collection<ArtifactCoords> extensions;
    private final boolean noJBangWrapper;

    public QuarkusJBangCodestartProjectInput(QuarkusJBangCodestartProjectInputBuilder builder) {
        super(builder);
        this.extensions = builder.extensions;
        this.noJBangWrapper = builder.noJBangWrapper;
    }

    public static QuarkusJBangCodestartProjectInputBuilder builder() {
        return new QuarkusJBangCodestartProjectInputBuilder();
    }

    public boolean noJBangWrapper() {
        return noJBangWrapper;
    }

    public Collection<ArtifactCoords> getExtensions() {
        return extensions;
    }

}
