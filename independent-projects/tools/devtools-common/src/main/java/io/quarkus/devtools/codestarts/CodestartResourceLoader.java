package io.quarkus.devtools.codestarts;

import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CodestartResourceLoader implements CodestartPathLoader {
    private ResourceLoader resourceLoader;

    private CodestartResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public static Map<String, Codestart> loadCodestartsFromResources(List<ResourceLoader> codestartResourceLoaders,
            String relativePath) throws IOException {
        final Map<String, Codestart> codestarts = new HashMap<>();
        for (ResourceLoader codestartResourceLoader : codestartResourceLoaders) {
            final CodestartPathLoader pathLoader = toCodestartPathLoader(codestartResourceLoader);
            final Collection<Codestart> loadedCodestarts = CodestartCatalogLoader.loadCodestarts(pathLoader, relativePath);
            for (Codestart codestart : loadedCodestarts) {
                codestarts.put(codestart.getName(), codestart);
            }
        }
        return codestarts;
    }

    public static CodestartPathLoader toCodestartPathLoader(ResourceLoader resourceLoader) {
        return new CodestartResourceLoader(resourceLoader);
    }

    @Override
    public <T> T loadResourceAsPath(String name, PathConsumer<T> consumer) throws IOException {
        return resourceLoader.loadResourceAsPath(name, consumer::consume);
    }
}
