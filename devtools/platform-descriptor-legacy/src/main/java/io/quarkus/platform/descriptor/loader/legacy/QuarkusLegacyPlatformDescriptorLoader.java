package io.quarkus.platform.descriptor.loader.legacy;

import io.quarkus.platform.descriptor.loader.QuarkusPlatformDescriptorLoader;
import io.quarkus.platform.descriptor.loader.QuarkusPlatformDescriptorLoaderContext;

public class QuarkusLegacyPlatformDescriptorLoader
        implements QuarkusPlatformDescriptorLoader<QuarkusLegacyPlatformDescriptor, QuarkusPlatformDescriptorLoaderContext> {

    @Override
    public QuarkusLegacyPlatformDescriptor load(QuarkusPlatformDescriptorLoaderContext context) {
        context.getMessageWriter().debug("Loading legacy Quarkus Core Platform descriptor");
        return new QuarkusLegacyPlatformDescriptor(Thread.currentThread().getContextClassLoader(), context.getMessageWriter());
    }
}
