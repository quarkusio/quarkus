package io.quarkus.platform.descriptor.loader.json.impl;

import java.io.IOException;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;

import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoader;
import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoaderContext;

public class QuarkusJsonPlatformDescriptorLoaderImpl
        implements QuarkusJsonPlatformDescriptorLoader<QuarkusJsonPlatformDescriptor> {

    @Override
    public QuarkusJsonPlatformDescriptor load(final QuarkusJsonPlatformDescriptorLoaderContext context) {

        final QuarkusJsonPlatformDescriptor platform = context
                .parseJson(is -> {
                    try {
                        ObjectMapper mapper = JsonMapper.builder()
                                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                                .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
                                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                                .propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
                                .build();
                        return mapper.readValue(is, QuarkusJsonPlatformDescriptor.class);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to parse JSON stream", e);
                    }
                });
        platform.setResourceLoader(context.getResourceLoader());
        platform.setMessageWriter(context.getMessageWriter());

        return platform;
    }
}
