package io.quarkus.registry.catalog.spi;

import io.quarkus.dependencies.Extension;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;

public interface IndexVisitor {

    void visitPlatform(QuarkusPlatformDescriptor platform);

    void visitExtension(Extension extension, String quarkusCore);
}
