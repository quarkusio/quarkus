package io.quarkus.platform.descriptor.loader.json;

import java.io.InputStream;
import java.util.function.Function;

import io.quarkus.platform.descriptor.loader.QuarkusPlatformDescriptorLoaderContext;
import io.quarkus.platform.tools.DefaultMessageWriter;
import io.quarkus.platform.tools.MessageWriter;

public abstract class QuarkusJsonPlatformDescriptorLoaderContext implements QuarkusPlatformDescriptorLoaderContext {

    protected final MessageWriter log;
    protected final ArtifactResolver artifactResolver;
    protected final ResourceLoader resourceLoader;

    public QuarkusJsonPlatformDescriptorLoaderContext(ArtifactResolver artifactResolver) {
        this(artifactResolver, new DefaultMessageWriter());
    }

    public QuarkusJsonPlatformDescriptorLoaderContext(ArtifactResolver artifactResolver, MessageWriter log) {
        this(artifactResolver, new ClassPathResourceLoader(Thread.currentThread().getContextClassLoader()), log);
    }

    public QuarkusJsonPlatformDescriptorLoaderContext(ArtifactResolver artifactResolver, ResourceLoader resourceLoader, MessageWriter log) {
        this.log = log;
        this.artifactResolver = artifactResolver;
        this.resourceLoader = resourceLoader;
    }

    public abstract <T> T parseJson(Function<InputStream, T> parser);

    @Override
    public MessageWriter getMessageWriter() {
        return log;
    }

    public ArtifactResolver getArtifactResolver() {
        return artifactResolver;
    }

    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }
}
