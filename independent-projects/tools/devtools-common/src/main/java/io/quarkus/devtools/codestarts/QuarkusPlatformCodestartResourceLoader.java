package io.quarkus.devtools.codestarts;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import java.io.IOException;

public final class QuarkusPlatformCodestartResourceLoader implements CodestartPathLoader {
    private QuarkusPlatformDescriptor platformDescr;

    private QuarkusPlatformCodestartResourceLoader(QuarkusPlatformDescriptor platformDescr) {
        this.platformDescr = platformDescr;
    }

    public static CodestartPathLoader platformPathLoader(QuarkusPlatformDescriptor platformDescr) {
        return new QuarkusPlatformCodestartResourceLoader(platformDescr);
    }

    @Override
    public <T> T loadResourceAsPath(String name, PathConsumer<T> consumer) throws IOException {
        return platformDescr.loadResourceAsPath(name, consumer::consume);
    }
}
