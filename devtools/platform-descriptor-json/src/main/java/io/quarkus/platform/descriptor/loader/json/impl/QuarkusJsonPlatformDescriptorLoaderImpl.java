package io.quarkus.platform.descriptor.loader.json.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoader;
import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoaderContext;

public class QuarkusJsonPlatformDescriptorLoaderImpl
        implements QuarkusJsonPlatformDescriptorLoader<QuarkusJsonPlatformDescriptor> {

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

        platform.setManagedDependencies(context.getArtifactResolver().getManagedDependencies(platform.getBomGroupId(),
                platform.getBomArtifactId(), null, "pom", platform.getBomVersion()));

        final Path classOrigin;
        try {
            classOrigin = MojoUtils.getClassOrigin(getClass());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to determine the origin of " + getClass().getName(), e);
        }

        final ResourceLoader resourceLoader;
        if (Files.isDirectory(classOrigin)) {
            resourceLoader = new DirectoryResourceLoader(classOrigin);
        } else {
            // this means the class belongs to a JAR which is on the classpath
            resourceLoader = new ClassPathResourceLoader(getClass().getClassLoader());
        }
        platform.setResourceLoader(resourceLoader);

        platform.setMessageWriter(context.getMessageWriter());

        return platform;
    }
}
