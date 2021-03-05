package io.quarkus.devtools.codestarts;

import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import java.io.IOException;

public final class QuarkusPlatformCodestartResourceLoader implements CodestartPathLoader {
    private ResourceLoader resourceLoader;

    private QuarkusPlatformCodestartResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public static CodestartPathLoader platformPathLoader(ResourceLoader resourceLoader) {
        return new QuarkusPlatformCodestartResourceLoader(resourceLoader);
    }

    @Override
    public <T> T loadResourceAsPath(String name, PathConsumer<T> consumer) throws IOException {
        return resourceLoader.loadResourceAsPath(name, consumer::consume);
    }
}
