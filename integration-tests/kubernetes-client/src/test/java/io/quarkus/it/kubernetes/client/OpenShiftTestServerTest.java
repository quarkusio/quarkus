package io.quarkus.it.kubernetes.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.openshift.api.model.ProjectBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.OpenShiftTestServer;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

@WithOpenShiftTestServer(setup = OpenShiftTestServerTest.CrudEnvironmentPreparation.class)
@QuarkusTest
class OpenShiftTestServerTest {

    @OpenShiftTestServer
    private OpenShiftServer mockServer;

    @Inject
    OpenShiftClient client;

    @Test
    void testInjectionDefaultsToCrud() {
        mockServer.getOpenshiftClient().projects().createOrReplace(new ProjectBuilder()
                .withNewMetadata().withName("example-project").addToLabels("project", "crud-is-true").endMetadata()
                .build());
        assertThat(client)
                .isNotSameAs(mockServer.getOpenshiftClient())
                .isNotSameAs(mockServer.getOpenshiftClient())
                .returns("crud-is-true",
                        c -> c.projects().withName("example-project").get().getMetadata().getLabels().get("project"));
    }

    public static final class CrudEnvironmentPreparation implements Consumer<OpenShiftServer> {

        @Override
        public void accept(OpenShiftServer openShiftServer) {
            final OpenShiftClient oc = openShiftServer.getOpenshiftClient();
            oc.configMaps().createOrReplace(new ConfigMapBuilder()
                    .withNewMetadata().withName("cmap1").endMetadata()
                    .addToData("dummy", "I'm required")
                    .build());
            oc.configMaps().createOrReplace(new ConfigMapBuilder()
                    .withNewMetadata().withName("cmap2").endMetadata()
                    .addToData("dummysecret", "dumb")
                    .addToData("overridden.secret", "Alex")
                    .addToData("some.prop1", "I'm required")
                    .addToData("some.prop2", "I'm required (2)")
                    .addToData("some.prop3", "I'm required (3)")
                    .addToData("some.prop4", "I'm required (4)")
                    .addToData("some.prop5", "I'm required (5)")
                    .build());
            oc.secrets().createOrReplace(new SecretBuilder()
                    .withNewMetadata().withName("s1").endMetadata()
                    .addToData("secret.prop1", encodeValue("s1cret"))
                    .addToData("secret.prop2", encodeValue("s2cret"))
                    .addToData("secret.prop3", encodeValue("s3cret"))
                    .addToData("secret.prop4", encodeValue("s4cret"))
                    .build());
        }
    }

    private static String encodeValue(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes());
    }
}
