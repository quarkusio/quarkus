package io.quarkus.kubernetes.client;

import java.util.List;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.quarkus.kubernetes.client.runtime.KubernetesClientProducer;
import io.quarkus.kubernetes.client.runtime.KubernetesConfigProducer;
import io.quarkus.kubernetes.client.runtime.internal.KubernetesClientBuildConfig;

/**
 * Meant to be implemented by a CDI bean that provided arbitrary customization for the default {@link Config} created by
 * Quarkus.
 * <p>
 * The {@link Config} is in turn used to produce the default {@link KubernetesClient}
 * <p>
 *
 * @see KubernetesConfigProducer#config(KubernetesClientBuildConfig, List)
 * @see KubernetesClientProducer#kubernetesClient(KubernetesSerialization, Config)
 */
public interface KubernetesConfigCustomizer {

    void customize(Config config);
}
