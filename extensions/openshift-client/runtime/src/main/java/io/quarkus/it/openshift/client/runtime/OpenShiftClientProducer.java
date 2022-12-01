package io.quarkus.it.openshift.client.runtime;

import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
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
    public OpenShiftClient openShiftClient(Config config) {
        // TODO - Temporary fix for https://github.com/fabric8io/kubernetes-client/pull/3347 + WithOpenShiftTestServer
        final OpenShiftConfig openShiftConfig = new OpenShiftConfig(config);
        openShiftConfig.setHttp2Disable(config.isHttp2Disable());
        client = new DefaultOpenShiftClient(openShiftConfig);
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
