package io.quarkus.platform.descriptor.loader;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;

public interface QuarkusPlatformDescriptorLoader<D extends QuarkusPlatformDescriptor, C extends QuarkusPlatformDescriptorLoaderContext> {

    D load(C context);
}
