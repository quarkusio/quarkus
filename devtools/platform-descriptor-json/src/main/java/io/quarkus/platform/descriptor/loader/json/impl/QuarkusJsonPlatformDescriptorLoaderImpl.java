package io.quarkus.platform.descriptor.loader.json.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoader;
import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoaderContext;

public class QuarkusJsonPlatformDescriptorLoaderImpl
        implements QuarkusJsonPlatformDescriptorLoader<QuarkusJsonPlatformDescriptor> {

    private static final String IO_QUARKUS = "io.quarkus";
    private static final String TEMPLATES_ARTIFACT_ID = "quarkus-devtools-templates";
    private static final String QUARKUS_CORE_ARTIFACT_ID = "quarkus-core";

    @Override
    public QuarkusJsonPlatformDescriptor load(QuarkusJsonPlatformDescriptorLoaderContext context) {
        context.getMessageWriter().debug("Loading Platform Descriptor from JSON %s", context.getJsonDescriptorFile());
        final QuarkusJsonPlatformDescriptor platform;
        try {
            ObjectMapper mapper = new ObjectMapper()
                    .enable(JsonParser.Feature.ALLOW_COMMENTS)
                    .enable(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS);
            try (InputStream is = Files.newInputStream(context.getJsonDescriptorFile())) {
                platform = mapper.readValue(is, QuarkusJsonPlatformDescriptor.class);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to load " + context.getJsonDescriptorFile(), e);
        }

        final DefaultArtifact platformBom = new DefaultArtifact(platform.getBomGroupId(), platform.getBomArtifactId(), null,
                "pom", platform.getBomVersion());
        String quarkusVersion = null;
        try {
            final List<Dependency> managedDeps = context.getMavenArtifactResolver().resolveDescriptor(platformBom)
                    .getManagedDependencies();
            final List<org.apache.maven.model.Dependency> convertedDeps = new ArrayList<>(managedDeps.size());
            for (Dependency dep : managedDeps) {
                final org.apache.maven.model.Dependency converted = new org.apache.maven.model.Dependency();
                convertedDeps.add(converted);
                final Artifact artifact = dep.getArtifact();
                converted.setGroupId(artifact.getGroupId());
                converted.setArtifactId(artifact.getArtifactId());
                converted.setVersion(artifact.getVersion());
                converted.setClassifier(artifact.getClassifier());
                converted.setType(artifact.getExtension());
                converted.setScope(dep.getScope());
                converted.setOptional(dep.isOptional());
                // exclusions aren't added yet

                if (artifact.getArtifactId().equals(QUARKUS_CORE_ARTIFACT_ID)
                        && artifact.getGroupId().equals(IO_QUARKUS)) {
                    quarkusVersion = artifact.getVersion();
                }
            }
            platform.setManagedDependencies(convertedDeps);
        } catch (AppModelResolverException e) {
            throw new RuntimeException("Failed to read descriptor of " + platformBom, e);
        }

        if (quarkusVersion == null) {
            throw new RuntimeException("Failed to determine the Quarkus version for the platform " + platformBom);
        }
        platform.setTemplatesJar(resolveArtifact(context.getMavenArtifactResolver(),
                new DefaultArtifact(IO_QUARKUS, TEMPLATES_ARTIFACT_ID, null, "jar", quarkusVersion)));
        platform.setQuarkusVersion(quarkusVersion);
        platform.setMessageWriter(context.getMessageWriter());

        return platform;
    }

    private static Path resolveArtifact(MavenArtifactResolver mvn, Artifact artifact) {
        try {
            return mvn.resolve(artifact).getArtifact().getFile().toPath();
        } catch (AppModelResolverException e) {
            throw new RuntimeException("Failed to resolve " + artifact, e);
        }
    }
}
