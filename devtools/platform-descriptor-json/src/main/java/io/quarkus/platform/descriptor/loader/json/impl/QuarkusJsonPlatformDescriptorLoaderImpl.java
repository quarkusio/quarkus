package io.quarkus.platform.descriptor.loader.json.impl;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoader;
import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoaderContext;

public class QuarkusJsonPlatformDescriptorLoaderImpl
        implements QuarkusJsonPlatformDescriptorLoader<QuarkusJsonPlatformDescriptor> {

    @Override
    public QuarkusJsonPlatformDescriptor load(final QuarkusJsonPlatformDescriptorLoaderContext context) {

        final QuarkusJsonPlatformDescriptor platform = context
                .parseJson(is -> {
                    try {
                        ObjectMapper mapper = new ObjectMapper()
                                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                                .enable(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS)
                                .setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
                        return mapper.readValue(is, QuarkusJsonPlatformDescriptor.class);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to parse JSON stream", e);
                    }
                });

        platform.setManagedDependencies(context.getArtifactResolver().getManagedDependencies(platform.getBomGroupId(),
                platform.getBomArtifactId(), null, "pom", platform.getBomVersion()));

        platform.setResourceLoader(context.getResourceLoader());
        platform.setMessageWriter(context.getMessageWriter());

        return platform;
    }
}
