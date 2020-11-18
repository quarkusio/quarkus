package io.quarkus.bootstrap.runner;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CapturingRunnerClassLoader extends RunnerClassLoader {

    private final Map<String, URL> capturedFindResource = new HashMap<>();
    private final Map<String, List<URL>> capturedFindResources = new HashMap<>();

    public CapturingRunnerClassLoader(ClassLoader parent, Map<String, ClassLoadingResource[]> resourceDirectoryMap,
            Set<String> parentFirstPackages, Set<String> nonExistentResources) {
        super(parent, resourceDirectoryMap, parentFirstPackages, nonExistentResources, Collections.emptyMap(),
                Collections.emptyMap());
    }

    @Override
    protected URL findResource(String name) {
        URL result = super.findResource(name);
        capturedFindResource.put(name, result);
        return result;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        Enumeration<URL> results = super.findResources(name);
        if ((results == null) || !results.hasMoreElements()) {
            capturedFindResources.put(name, Collections.emptyList());
            return results;
        }
        List<URL> list = Collections.list(results);
        capturedFindResources.put(name, list);
        return Collections.enumeration(list); // convert back to enumeration because the original one has already been consumed
    }

    public Map<String, URL> getCapturedFindResource() {
        return capturedFindResource;
    }

    public Map<String, List<URL>> getCapturedFindResources() {
        return capturedFindResources;
    }
}
