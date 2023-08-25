package io.quarkus.it.openshift.client.runtime;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Specializes;
import jakarta.inject.Singleton;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.arc.DefaultBean;
import io.quarkus.kubernetes.client.runtime.KubernetesClientProducer;

@Singleton
public class OpenShiftClientProducer /* extends KubernetesClientProducer */ { // If we keep the extends, then the parent's class method is registered as an extension

    // Temporary delegation workaround to illustrate how the behavior should be
    private KubernetesClientProducer delegate;

    @Specializes // Annotation is ignored (at class level and method level). Both, the parent's class method producer and
    @DefaultBean
    @Singleton
    @Produces
    //    @Override
    public OpenShiftClient kubernetesClient(KubernetesSerialization kubernetesSerialization, Config config) {
        delegate = new KubernetesClientProducer();
        return delegate.kubernetesClient(kubernetesSerialization, config).adapt(OpenShiftClient.class);
    }

    // Temporary workaround to illustrate how the behavior should be when using specialization
    // This method shouldn't be necessary if specialization works as expected
    @PreDestroy
    public void destroy() {
        if (delegate != null) {
            delegate.destroy();
        }
    }
}
