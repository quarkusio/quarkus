package io.quarkus.kubernetes.client.deployment;

import jakarta.inject.Inject;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.QuarkusUnitTest;

public class KubernetesClientProxyConfigHttpTest {

    @Inject
    KubernetesClient kubernetesClient;

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("using-http-proxy-configuration.properties");

    @Test
    public void usingSameNamedProxyConfiguration() {
        SoftAssertions.assertSoftly(softly -> {
            Config configuration = kubernetesClient.getConfiguration();
            softly.assertThat(configuration.getHttpProxy()).isEqualTo("http://localhost:8080");
            softly.assertThat(configuration.getHttpsProxy()).isNull();
            softly.assertThat(configuration.getNoProxy()).contains("kubernetes-client-test.com", "api.example.com");
        });
    }
}
