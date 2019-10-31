package io.quarkus.platform.descriptor.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.apache.maven.model.Dependency;
import org.junit.jupiter.api.Test;

import io.quarkus.platform.descriptor.loader.json.ArtifactResolver;
import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoader;
import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoaderContext;
import io.quarkus.platform.descriptor.loader.json.impl.QuarkusJsonPlatformDescriptor;
import io.quarkus.platform.descriptor.loader.json.impl.QuarkusJsonPlatformDescriptorLoaderImpl;

class PlatformDescriptorLoaderTest {

    @Test
    void test() {

        QuarkusJsonPlatformDescriptorLoader<QuarkusJsonPlatformDescriptor> qpd = new QuarkusJsonPlatformDescriptorLoaderImpl();

        final ArtifactResolver artifactResolver = new ArtifactResolver() {

            @Override
            public <T> T process(String groupId, String artifactId, String classifier, String type, String version,
                    Function<Path, T> processor) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<Dependency> getManagedDependencies(String groupId, String artifactId, String classifier,
                    String type, String version) {
                return Collections.emptyList();
            }
        };

        QuarkusJsonPlatformDescriptorLoaderContext context = new QuarkusJsonPlatformDescriptorLoaderContext(artifactResolver) {
            @Override
            public <T> T parseJson(Function<InputStream, T> parser) {
                String resourceName = "fakeextensions.json";

                final InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName);
                if (is == null) {
                    throw new IllegalStateException("Failed to locate " + resourceName + " on the classpath");
                }

                try {
                    return parser.apply(is);
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
            }

        };

        QuarkusJsonPlatformDescriptor load = qpd.load(context);

        assertNotNull(load);

        assertEquals(85, load.getExtensions().size());

        assertEquals(1, load.getCategories().size());

    }

}
