package io.quarkus.platform.descriptor.loader.json.util;

import java.nio.file.Path;

import org.eclipse.aether.artifact.Artifact;

import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoaderContext;
import io.quarkus.platform.descriptor.loader.json.impl.QuarkusJsonPlatformDescriptor;
import io.quarkus.platform.descriptor.loader.json.impl.QuarkusJsonPlatformDescriptorLoaderImpl;
import io.quarkus.platform.tools.DefaultMessageWriter;
import io.quarkus.platform.tools.MessageWriter;

public class QuarkusJsonDescriptorUtils {

    public static QuarkusJsonPlatformDescriptor loadDescriptor(MavenArtifactResolver mvn, Artifact jsonArtifact) {
        return loadDescriptor(mvn, jsonArtifact, new DefaultMessageWriter());
    }

    public static QuarkusJsonPlatformDescriptor loadDescriptor(MavenArtifactResolver mvn, Artifact jsonArtifact,
            final MessageWriter log) {
        try {
            jsonArtifact = mvn.resolve(jsonArtifact).getArtifact();
        } catch (AppModelResolverException e1) {
            throw new RuntimeException("Failed to resolve Quarkus platform JSON descriptor " + jsonArtifact, e1);
        }
        final Path platformJson = jsonArtifact.getFile().toPath();

        log.debug("Quarkus platform JSON descriptor %s", jsonArtifact);
        return new QuarkusJsonPlatformDescriptorLoaderImpl()
                .load(new QuarkusJsonPlatformDescriptorLoaderContext() {

                    @Override
                    public MessageWriter getMessageWriter() {
                        return log;
                    }

                    @Override
                    public Path getJsonDescriptorFile() {
                        return platformJson;
                    }

                    @Override
                    public MavenArtifactResolver getMavenArtifactResolver() {
                        return mvn;
                    }
                });
    }
}
