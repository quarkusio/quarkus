package io.quarkus.infinispan.embedded.runtime;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

@ApplicationScoped
public class InfinispanEmbeddedProducer {

    private InfinispanEmbeddedRuntimeConfig config;

    public void setRuntimeConfig(InfinispanEmbeddedRuntimeConfig config) {
        this.config = config;
    }

    @Produces
    EmbeddedCacheManager manager() {
        if (config.xmlConfig.isPresent()) {
            try {
                return new DefaultCacheManager(config.xmlConfig.get());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new DefaultCacheManager();
    }
}
