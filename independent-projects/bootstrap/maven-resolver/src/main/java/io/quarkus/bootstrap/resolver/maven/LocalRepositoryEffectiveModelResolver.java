package io.quarkus.bootstrap.resolver.maven;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.eclipse.aether.repository.RemoteRepository;

import io.quarkus.maven.dependency.ArtifactCoords;

class LocalRepositoryEffectiveModelResolver implements EffectiveModelResolver {

    private final LocalRepoModelResolver modelResolver;

    LocalRepositoryEffectiveModelResolver(File localRepoDir) {
        modelResolver = LocalRepoModelResolver.of(new MavenLocalPomResolver(localRepoDir));
    }

    @Override
    public Model resolveEffectiveModel(ArtifactCoords coords) {
        return resolveEffectiveModel(coords, List.of());
    }

    @Override
    public Model resolveEffectiveModel(ArtifactCoords coords, List<RemoteRepository> repos) {
        File pom = modelResolver.resolvePom(coords.getGroupId(), coords.getArtifactId(), coords.getVersion());
        if (pom == null) {
            return null;
        }
        ModelBuildingRequest req = new DefaultModelBuildingRequest();
        req.setModelResolver(modelResolver);
        req.setPomFile(pom);
        req.getSystemProperties().putAll(System.getProperties());
        req.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);

        // execute the model building request
        DefaultModelBuilderFactory factory = new DefaultModelBuilderFactory();
        DefaultModelBuilder builder = factory.newInstance();
        try {
            return builder.build(req).getEffectiveModel();
        } catch (ModelBuildingException e) {
            throw new RuntimeException("An error occurred attempting to resolve effective POM", e);
        }
    }

}
