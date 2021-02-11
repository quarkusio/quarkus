package io.quarkus.devtools.codestarts.jbang;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.devtools.codestarts.CodestartProjectInput;
import java.util.Collection;

public final class QuarkusJBangCodestartProjectInput extends CodestartProjectInput {
    private final Collection<AppArtifactCoords> extensions;
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

    public Collection<AppArtifactCoords> getExtensions() {
        return extensions;
    }

}
