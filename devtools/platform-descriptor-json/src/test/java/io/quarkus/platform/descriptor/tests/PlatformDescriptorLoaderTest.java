package io.quarkus.platform.descriptor.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
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
import io.quarkus.platform.tools.DefaultMessageWriter;
import io.quarkus.platform.tools.MessageWriter;

class PlatformDescriptorLoaderTest {

    @Test
    void test() {

        QuarkusJsonPlatformDescriptorLoader<QuarkusJsonPlatformDescriptor> qpd = new QuarkusJsonPlatformDescriptorLoaderImpl();

        QuarkusJsonPlatformDescriptorLoaderContext context = new QuarkusJsonPlatformDescriptorLoaderContext() {

            MessageWriter mw = new DefaultMessageWriter();

            @Override
            public MessageWriter getMessageWriter() {
                return mw;
            }

            @Override
            public ArtifactResolver getArtifactResolver() {
                return new ArtifactResolver() {

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
            }

            @Override
            public <T> T parseJson(Function<Path, T> parser) {
                String resourceName = "fakeextensions.json";

                ClassLoader classLoader = getClass().getClassLoader();
                File file = new File(classLoader.getResource(resourceName).getFile());

                return parser.apply(file.toPath());

            }

        };

        QuarkusJsonPlatformDescriptor load = qpd.load(context);

        assertNotNull(load);

        assertEquals(85, load.getExtensions().size());

        assertEquals(1, load.getCategories().size());

    }

}
