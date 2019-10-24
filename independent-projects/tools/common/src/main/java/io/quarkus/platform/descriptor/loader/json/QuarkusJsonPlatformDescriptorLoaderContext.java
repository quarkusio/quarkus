package io.quarkus.platform.descriptor.loader.json;

import java.nio.file.Path;
import java.util.function.Function;

import io.quarkus.platform.descriptor.loader.QuarkusPlatformDescriptorLoaderContext;

public interface QuarkusJsonPlatformDescriptorLoaderContext extends QuarkusPlatformDescriptorLoaderContext {

    <T> T parseJson(Function<Path, T> parser);

    ArtifactResolver getArtifactResolver();
}
