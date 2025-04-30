package io.quarkus.bootstrap.resolver.maven;

import java.util.List;

import org.apache.maven.model.Model;
import org.eclipse.aether.repository.RemoteRepository;

import io.quarkus.maven.dependency.ArtifactCoords;

public interface EffectiveModelResolver {

    static EffectiveModelResolver of(MavenArtifactResolver resolver) {
        return new DefaultEffectiveModelResolver(resolver);
    }

    default Model resolveEffectiveModel(ArtifactCoords coords) {
        return resolveEffectiveModel(coords, List.of());
    }

    Model resolveEffectiveModel(ArtifactCoords coords, List<RemoteRepository> repos);
}
