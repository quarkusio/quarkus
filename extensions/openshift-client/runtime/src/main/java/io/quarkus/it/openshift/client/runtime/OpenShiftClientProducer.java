package io.quarkus.it.openshift.client.runtime;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.OpenShiftConfig;
import io.quarkus.arc.DefaultBean;

@Singleton
public class OpenShiftClientProducer {

    private static final Logger LOGGER = Logger.getLogger(OpenShiftClientProducer.class);

    private OpenShiftClient client;

    @DefaultBean
    @Singleton
    @Produces
    public OpenShiftClient openShiftClient(KubernetesSerialization kubernetesSerialization,
            Config config) {
        // TODO - Temporary fix for https://github.com/fabric8io/kubernetes-client/pull/3347 + WithOpenShiftTestServer
        final OpenShiftConfig openShiftConfig = new OpenShiftConfig(config);
        openShiftConfig.setHttp2Disable(config.isHttp2Disable());

        client = new KubernetesClientBuilder()
                .withConfig(openShiftConfig)
                .withKubernetesSerialization(kubernetesSerialization)
                .build()
                .adapt(OpenShiftClient.class);

        return client;
    }

    @PreDestroy
    public void destroy() {
        if (client != null) {
            LOGGER.info("Closing OpenShift client");
            client.close();
        }
    }
}
