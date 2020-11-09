package io.quarkus.platform.descriptor.resolver.json.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.dependencies.Category;
import io.quarkus.dependencies.Extension;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.ResourceInputStreamConsumer;
import io.quarkus.platform.descriptor.ResourcePathConsumer;
import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoader;
import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoaderContext;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class TestJsonPlatformDescriptorLoader implements QuarkusJsonPlatformDescriptorLoader<QuarkusPlatformDescriptor> {

    @Override
    public QuarkusPlatformDescriptor load(QuarkusJsonPlatformDescriptorLoaderContext context) {
        final JsonNode json = context.parseJson(s -> {
            try (InputStreamReader reader = new InputStreamReader(s, StandardCharsets.UTF_8)) {
                return new ObjectMapper().readTree(reader);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to parse JSON descriptor", e);
            }
        });

        final JsonNode bom = json.required("bom");
        final String quarkusVersion = json.required("quarkus-core-version").asText();

        return new QuarkusPlatformDescriptor() {

            @Override
            public String getBomGroupId() {
                return bom.get("groupId").asText(null);
            }

            @Override
            public String getBomArtifactId() {
                return bom.get("artifactId").asText(null);
            }

            @Override
            public String getBomVersion() {
                return bom.get("version").asText(null);
            }

            @Override
            public String getQuarkusVersion() {
                return quarkusVersion;
            }

            @Override
            public List<Extension> getExtensions() {
                return Collections.emptyList();
            }

            @Override
            public List<Category> getCategories() {
                return Collections.emptyList();
            }

            @Override
            public String getTemplate(String name) {
                return null;
            }

            @Override
            public <T> T loadResource(String name, ResourceInputStreamConsumer<T> consumer) throws IOException {
                return null;
            }

            @Override
            public <T> T loadResourceAsPath(String name, ResourcePathConsumer<T> consumer) throws IOException {
                return null;
            }
        };
    }
}
