package io.quarkus.platform.descriptor.loader.json.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;

import org.apache.maven.model.Dependency;

import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.loader.QuarkusPlatformDescriptorLoader;
import io.quarkus.platform.descriptor.loader.QuarkusPlatformDescriptorLoaderContext;
import io.quarkus.platform.descriptor.loader.json.ArtifactResolver;
import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoaderContext;

/**
 * This class is used to bootstrap the Quarkus platform descriptor from the classpath only.
 */
public class QuarkusJsonPlatformDescriptorLoaderBootstrap
        implements QuarkusPlatformDescriptorLoader<QuarkusPlatformDescriptor, QuarkusPlatformDescriptorLoaderContext> {

    private static InputStream getResourceStream(String relativePath) {
        final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(relativePath);
        if (is == null) {
            throw new IllegalStateException("Failed to locate " + relativePath + " on the classpath");
        }
        return is;
    }

    @Override
    public QuarkusPlatformDescriptor load(QuarkusPlatformDescriptorLoaderContext context) {

        context.getMessageWriter().debug("Loading the default Quarkus Platform descriptor from the classpath");

        final Properties props = new Properties();
        final InputStream quarkusProps = getResourceStream("quarkus.properties");
        try {
            props.load(quarkusProps);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load properties quarkus.properties", e);
        }

        final Path resourceRoot;
        try {
            resourceRoot = MojoUtils.getClassOrigin(QuarkusJsonPlatformDescriptorLoaderBootstrap.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to determine the resource root for " + getClass().getName(), e);
        }

        final ArtifactResolver resolver = new ArtifactResolver() {
            @Override
            public <T> T process(String groupId, String artifactId, String classifier, String type, String version,
                    Function<Path, T> processor) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<Dependency> getManagedDependencies(String groupId, String artifactId, String classifier, String type,
                    String version) {
                if (Files.isDirectory(resourceRoot)) {
                    return readManagedDeps(resourceRoot.resolve("quarkus-bom/pom.xml"));
                }
                try (FileSystem fs = FileSystems.newFileSystem(resourceRoot, null)) {
                    return readManagedDeps(fs.getPath("/quarkus-bom/pom.xml"));
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to open " + resourceRoot, e);
                }
            }
        };

        return new QuarkusJsonPlatformDescriptorLoaderImpl()
                .load(new QuarkusJsonPlatformDescriptorLoaderContext(resolver, context.getMessageWriter()) {
                    @Override
                    public <T> T parseJson(Function<InputStream, T> parser) {
                        if (Files.isDirectory(resourceRoot)) {
                            return doParse(resourceRoot.resolve("quarkus-bom-descriptor/extensions.json"), parser);
                        }
                        try (FileSystem fs = FileSystems.newFileSystem(resourceRoot, null)) {
                            return doParse(fs.getPath("/quarkus-bom-descriptor/extensions.json"), parser);
                        } catch (IOException e) {
                            throw new IllegalStateException("Failed to open " + resourceRoot, e);
                        }
                    }
                });
    }

    private static List<Dependency> readManagedDeps(Path pom) {
        if (!Files.exists(pom)) {
            throw new IllegalStateException("Failed to locate " + pom);
        }
        try (InputStream is = Files.newInputStream(pom)) {
            return MojoUtils.readPom(is).getDependencyManagement().getDependencies();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read model of " + pom, e);
        }
    }

    private static <T> T doParse(Path p, Function<InputStream, T> parser) {
        if (!Files.exists(p)) {
            throw new IllegalStateException("Path does not exist: " + p);
        }
        try (InputStream is = Files.newInputStream(p)) {
            return parser.apply(is);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + p, e);
        }
    }
}
