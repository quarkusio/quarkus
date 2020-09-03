package io.quarkus.platform.descriptor.loader.json;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.platform.descriptor.loader.QuarkusPlatformDescriptorLoaderContext;
import java.io.InputStream;
import java.util.function.Function;

public abstract class QuarkusJsonPlatformDescriptorLoaderContext implements QuarkusPlatformDescriptorLoaderContext {

    protected final MessageWriter log;
    protected final ArtifactResolver artifactResolver;
    protected final ResourceLoader resourceLoader;

    public QuarkusJsonPlatformDescriptorLoaderContext(ArtifactResolver artifactResolver) {
        this(artifactResolver, MessageWriter.info());
    }

    public QuarkusJsonPlatformDescriptorLoaderContext(ArtifactResolver artifactResolver, MessageWriter log) {
        this(artifactResolver, new ClassPathResourceLoader(Thread.currentThread().getContextClassLoader()), log);
    }

    public QuarkusJsonPlatformDescriptorLoaderContext(ArtifactResolver artifactResolver, ResourceLoader resourceLoader,
            MessageWriter log) {
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
