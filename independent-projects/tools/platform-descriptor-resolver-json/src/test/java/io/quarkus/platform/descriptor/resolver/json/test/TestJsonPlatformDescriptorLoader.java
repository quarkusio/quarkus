package io.quarkus.platform.descriptor.resolver.json.test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Dependency;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import io.quarkus.dependencies.Category;
import io.quarkus.dependencies.Extension;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.ResourceInputStreamConsumer;
import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoader;
import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoaderContext;

public class TestJsonPlatformDescriptorLoader implements QuarkusJsonPlatformDescriptorLoader<QuarkusPlatformDescriptor> {

    @Override
    public QuarkusPlatformDescriptor load(QuarkusJsonPlatformDescriptorLoaderContext context) {
        final JsonObject json = context.parseJson(s -> {
            try (InputStreamReader reader = new InputStreamReader(s, StandardCharsets.UTF_8)) {
                return Json.parse(reader).asObject();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to parse JSON descriptor", e);
            }
        });

        JsonValue jsonValue = getRequiredAttr(json, "bom");
        final JsonObject bom = jsonValue.asObject();

        jsonValue = getRequiredAttr(json, "quarkus-core-version");
        final String quarkusVersion = jsonValue.asString();

        return new QuarkusPlatformDescriptor() {

            @Override
            public String getBomGroupId() {
                return bom.getString("groupId", null);
            }

            @Override
            public String getBomArtifactId() {
                return bom.getString("artifactId", null);
            }

            @Override
            public String getBomVersion() {
                return bom.getString("version", null);
            }

            @Override
            public String getQuarkusVersion() {
                return quarkusVersion;
            }

            @Override
            public List<Dependency> getManagedDependencies() {
                return Collections.emptyList();
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
            }};
    }

    private JsonValue getRequiredAttr(final JsonObject json, String name) {
        final JsonValue jsonValue = json.get(name);
        if(jsonValue == null) {
            throw new IllegalStateException("Failed to locate '" + name + "' attribute in the JSON descriptor");
        }
        return jsonValue;
    }
}
