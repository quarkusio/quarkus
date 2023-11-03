package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.Subject;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithRbacAndServiceAccountTest {

    private static final String APP_NAME = "kubernetes-with-rbac-and-service-account";
    private static final String SERVICE_ACCOUNT = "my-service-account";
    private static final String ROLE = "my-role";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource(APP_NAME + ".properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        final Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        Deployment deployment = getDeploymentByName(kubernetesList, APP_NAME);
        assertThat(deployment.getSpec().getTemplate().getSpec().getServiceAccountName()).isEqualTo(SERVICE_ACCOUNT);

        // my-role assertions
        Role myRole = getRoleByName(kubernetesList, ROLE);
        assertThat(myRole.getRules()).satisfiesOnlyOnce(r -> {
            assertThat(r.getApiGroups()).containsExactly("extensions", "apps");
            assertThat(r.getResources()).containsExactly("deployments");
            assertThat(r.getVerbs()).containsExactly("get", "watch", "list");
        });

        // service account
        ServiceAccount serviceAccount = getServiceAccountByName(kubernetesList, SERVICE_ACCOUNT);
        assertThat(serviceAccount).isNotNull();

        // role binding
        RoleBinding roleBinding = getRoleBindingByName(kubernetesList, "my-role-binding");
        assertEquals("Role", roleBinding.getRoleRef().getKind());
        assertEquals(ROLE, roleBinding.getRoleRef().getName());
        Subject subject = roleBinding.getSubjects().get(0);
        assertEquals("ServiceAccount", subject.getKind());
        assertEquals(SERVICE_ACCOUNT, subject.getName());
    }

    private Deployment getDeploymentByName(List<HasMetadata> kubernetesList, String name) {
        return getResourceByName(kubernetesList, Deployment.class, name);
    }

    private Role getRoleByName(List<HasMetadata> kubernetesList, String roleName) {
        return getResourceByName(kubernetesList, Role.class, roleName);
    }

    private ServiceAccount getServiceAccountByName(List<HasMetadata> kubernetesList, String saName) {
        return getResourceByName(kubernetesList, ServiceAccount.class, saName);
    }

    private RoleBinding getRoleBindingByName(List<HasMetadata> kubernetesList, String rbName) {
        return getResourceByName(kubernetesList, RoleBinding.class, rbName);
    }

    private <T extends HasMetadata> T getResourceByName(List<HasMetadata> kubernetesList, Class<T> clazz, String name) {
        Optional<T> resource = kubernetesList.stream()
                .filter(r -> r.getMetadata().getName().equals(name))
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .findFirst();

        assertTrue(resource.isPresent(), name + " resource not found!");
        return resource.get();
    }
}
