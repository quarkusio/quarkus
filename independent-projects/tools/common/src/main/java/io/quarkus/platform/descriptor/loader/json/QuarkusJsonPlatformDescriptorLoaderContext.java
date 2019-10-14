package io.quarkus.platform.descriptor.loader.json;

import java.nio.file.Path;

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.platform.descriptor.loader.QuarkusPlatformDescriptorLoaderContext;

public interface QuarkusJsonPlatformDescriptorLoaderContext extends QuarkusPlatformDescriptorLoaderContext {

    Path getJsonDescriptorFile();

    MavenArtifactResolver getMavenArtifactResolver();
}
