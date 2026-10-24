package io.quarkus.gradle.model.pom;

import java.util.Map;
import java.util.function.Supplier;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;

import io.quarkus.maven.dependency.GAV;

class MavenEffectiveModelResolver {

    private final PomResolver pomResolver;
    private final Supplier<Map<String, String>> systemProperties;
    private final ModelResolver modelResolver;
    private final DefaultModelBuilder modelBuilder;

    MavenEffectiveModelResolver(PomResolver pomResolver, Supplier<Map<String, String>> systemProperties) {
        this.pomResolver = pomResolver;
        this.systemProperties = systemProperties;
        this.modelResolver = new PomBackedModelResolver(pomResolver);
        this.modelBuilder = new DefaultModelBuilderFactory().newInstance();
    }

    Model resolveEffectiveModel(String groupId, String artifactId, String version)
            throws UnresolvableModelException, ModelBuildingException {
        var request = new DefaultModelBuildingRequest();
        request.setModelSource(pomResolver.resolvePom(new GAV(groupId, artifactId, version)));
        request.setModelResolver(modelResolver);
        request.getSystemProperties().putAll(systemProperties.get());
        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);

        // Maven XML discovers its implementation through the TCCL. Gradle invokes this provider with a classloader
        // that cannot see the model module's runtime dependencies, including Maven's XML service implementation.
        var thread = Thread.currentThread();
        var originalTccl = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(getClass().getClassLoader());
            return modelBuilder.build(request).getEffectiveModel();
        } finally {
            thread.setContextClassLoader(originalTccl);
        }
    }

    private record PomBackedModelResolver(PomResolver pomResolver) implements ModelResolver {
        @Override
        public ModelSource2 resolveModel(String groupId, String artifactId, String version)
                throws UnresolvableModelException {
            return pomResolver.resolvePom(new GAV(groupId, artifactId, version));
        }

        @Override
        public ModelSource2 resolveModel(Parent parent) throws UnresolvableModelException {
            return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
        }

        @Override
        public ModelSource2 resolveModel(Dependency dependency) throws UnresolvableModelException {
            return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
        }

        @Override
        public void addRepository(Repository repository) {
            // The Gradle-backed resolver intentionally uses repositories configured in Gradle.
        }

        @Override
        public void addRepository(Repository repository, boolean replace) {
            // The Gradle-backed resolver intentionally uses repositories configured in Gradle.
        }

        @Override
        public ModelResolver newCopy() {
            return this;
        }
    }
}
