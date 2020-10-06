package io.quarkus.kubernetes.client.runtime;

import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.arc.DefaultBean;

@Singleton
public class KubernetesClientProducer {

    private static final Logger LOGGER = Logger.getLogger(KubernetesClientProducer.class);

    private KubernetesClient client;

    @DefaultBean
    @Singleton
    @Produces
    public Config config(KubernetesClientBuildConfig buildConfig) {
        return KubernetesClientUtils.createConfig(buildConfig);
    }

    @DefaultBean
    @Singleton
    @Produces
    public KubernetesClient kubernetesClient(Config config) {
        client = new DefaultKubernetesClient(config);
        return client;
    }

    @PreDestroy
    public void destroy() {
        if (client != null) {
            LOGGER.info("Closing Kubernetes client");
            client.close();
        }
    }
}
