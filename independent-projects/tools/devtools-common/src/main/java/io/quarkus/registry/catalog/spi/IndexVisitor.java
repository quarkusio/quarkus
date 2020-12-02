package io.quarkus.registry.catalog.spi;

import io.quarkus.dependencies.Extension;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.registry.model.Release;

public interface IndexVisitor {

    void visitPlatform(QuarkusPlatformDescriptor platform);

    void visitExtension(Extension extension, String quarkusCore);

    void visitExtensionRelease(String groupId, String artifactId, Release release);
}
