package io.quarkus.platform.descriptor.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
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
            public MavenArtifactResolver getMavenArtifactResolver() {
                try {
                    return MavenArtifactResolver.builder()
                            .setRepoHome(new File("~/.m2").toPath())
                            .setOffline(true)
                            .build();
                } catch (AppModelResolverException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public Path getJsonDescriptorFile() {
                String resourceName = "fakeextensions.json";

                ClassLoader classLoader = getClass().getClassLoader();
                File file = new File(classLoader.getResource(resourceName).getFile());

                return file.toPath();
            }
        };

        QuarkusJsonPlatformDescriptor load = qpd.load(context);

        assertNotNull(load);

        assertEquals(85, load.getExtensions().size());

        assertEquals(1, load.getCategories().size());

    }

}
