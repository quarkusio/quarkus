package io.quarkus.platform.descriptor.loader.json.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.eclipse.aether.artifact.DefaultArtifact;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoader;
import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoaderContext;

public class QuarkusJsonPlatformDescriptorLoaderImpl
        implements QuarkusJsonPlatformDescriptorLoader<QuarkusJsonPlatformDescriptor> {

    private static final String IO_QUARKUS = "io.quarkus";
    private static final String QUARKUS_CORE_ARTIFACT_ID = "quarkus-core";

    @Override
    public QuarkusJsonPlatformDescriptor load(final QuarkusJsonPlatformDescriptorLoaderContext context) {

        final QuarkusJsonPlatformDescriptor platform = context
                .parseJson(p -> {
                    context.getMessageWriter().debug("Loading Platform Descriptor from JSON %s", p);
                    try {
                        ObjectMapper mapper = new ObjectMapper()
                                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                                .enable(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS)
                                .setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
                        try (InputStream is = Files.newInputStream(p)) {
                            return mapper.readValue(is, QuarkusJsonPlatformDescriptor.class);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Unable to load " + p, e);
                    }
                });

        final DefaultArtifact platformBom = new DefaultArtifact(platform.getBomGroupId(), platform.getBomArtifactId(), null,
                "pom", platform.getBomVersion());
        String quarkusVersion = null;
        try {
            final List<Dependency> managedDeps = context.getArtifactResolver().getManagedDependencies(platformBom.getGroupId(),
                    platformBom.getArtifactId(), platformBom.getVersion());
            final List<Dependency> convertedDeps = new ArrayList<>(managedDeps.size());
            for (Dependency dep : managedDeps) {
                final org.apache.maven.model.Dependency converted = new org.apache.maven.model.Dependency();
                convertedDeps.add(converted);
                converted.setGroupId(dep.getGroupId());
                converted.setArtifactId(dep.getArtifactId());
                converted.setVersion(dep.getVersion());
                converted.setClassifier(dep.getClassifier());
                converted.setType(dep.getType());
                converted.setScope(dep.getScope());
                converted.setOptional(dep.isOptional());
                // exclusions aren't added yet

                if (dep.getArtifactId().equals(QUARKUS_CORE_ARTIFACT_ID)
                        && dep.getGroupId().equals(IO_QUARKUS)) {
                    quarkusVersion = dep.getVersion();
                }
            }
            platform.setManagedDependencies(convertedDeps);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read descriptor of " + platformBom, e);
        }

        if (quarkusVersion == null) {
            throw new RuntimeException("Failed to determine the Quarkus version for the platform " + platformBom);
        }

        try {
            platform.setTemplatesJar(MojoUtils.getClassOrigin(getClass()));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to determine the origin of " + getClass().getName(), e);
        }
        platform.setQuarkusVersion(quarkusVersion);
        platform.setMessageWriter(context.getMessageWriter());

        return platform;
    }
}
