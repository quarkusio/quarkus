package io.quarkus.kubernetes.client.deployment;

import jakarta.inject.Inject;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.QuarkusUnitTest;

public class KubernetesClientProxyConfigHttpsTest {

    @Inject
    KubernetesClient kubernetesClient;

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("using-https-proxy-configuration.properties");

    @Test
    public void usingSameNamedProxyConfiguration() {
        SoftAssertions.assertSoftly(softly -> {
            Config configuration = kubernetesClient.getConfiguration();
            softly.assertThat(configuration.getHttpsProxy()).isEqualTo("https://secure.localhost:8443");
            softly.assertThat(configuration.getHttpProxy()).isNull();
            softly.assertThat(configuration.getNoProxy()).contains("secure.kubernetes-client-test.com",
                    "secure.api.example.com");
        });
    }
}
