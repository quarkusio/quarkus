package io.quarkus.undertow.runtime;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;

public class DelegatingResourceManager implements ResourceManager {

    private final List<ResourceManager> delegates;

    public DelegatingResourceManager(ResourceManager... delegates) {
        this.delegates = Arrays.asList(delegates);
    }

    @Override
    public Resource getResource(String path) throws IOException {
        for (ResourceManager i : delegates) {
            Resource res = i.getResource(path);
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    @Override
    public boolean isResourceChangeListenerSupported() {
        return false;
    }

    @Override
    public void registerResourceChangeListener(ResourceChangeListener listener) {

    }

    @Override
    public void removeResourceChangeListener(ResourceChangeListener listener) {

    }

    @Override
    public void close() throws IOException {
        for (ResourceManager i : delegates) {
            i.close();
        }
    }
}
