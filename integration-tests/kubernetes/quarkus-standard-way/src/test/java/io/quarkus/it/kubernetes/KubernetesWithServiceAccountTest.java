package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithServiceAccountTest {

    private static final String APP_NAME = "kubernetes-with-service-account";
    private static final String SERVICE_ACCOUNT = "my-service-account";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.kubernetes.service-account", SERVICE_ACCOUNT);

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        final Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        Deployment deployment = getDeploymentByName(kubernetesList, APP_NAME).get();
        assertThat(deployment.getSpec().getTemplate().getSpec().getServiceAccountName()).isEqualTo(SERVICE_ACCOUNT);

        Optional<ServiceAccount> serviceAccount = getServiceAccountByName(kubernetesList, SERVICE_ACCOUNT);
        assertThat(serviceAccount.isPresent()).isFalse();
    }

    private Optional<Deployment> getDeploymentByName(List<HasMetadata> kubernetesList, String name) {
        return getResourceByName(kubernetesList, Deployment.class, name);
    }

    private Optional<ServiceAccount> getServiceAccountByName(List<HasMetadata> kubernetesList, String saName) {
        return getResourceByName(kubernetesList, ServiceAccount.class, saName);
    }

    private <T extends HasMetadata> Optional<T> getResourceByName(List<HasMetadata> kubernetesList, Class<T> clazz,
            String name) {
        return kubernetesList.stream()
                .filter(r -> r.getMetadata().getName().equals(name))
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .findFirst();
    }
}
