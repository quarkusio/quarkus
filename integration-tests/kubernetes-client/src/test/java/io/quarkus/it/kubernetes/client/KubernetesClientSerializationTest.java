package io.quarkus.it.kubernetes.client;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.function.Consumer;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesServer;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.restassured.filter.log.LogDetail;

@WithKubernetesTestServer(setup = KubernetesClientSerializationTest.CrudEnvironmentPreparation.class)
@QuarkusTest
public class KubernetesClientSerializationTest {

    @KubernetesTestServer
    private KubernetesServer mockServer;

    @Test
    @DisplayName("PUT with String body - Should serialize and persist the Pod")
    void serialization() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "apiVersion": "v1",
                          "kind": "Pod",
                          "metadata": {
                            "name": "serialization-test"
                          },
                          "spec": {
                            "containers": [
                              {
                                "name": "test",
                                "image": "busybox",
                                "volumeMounts": [
                                  {
                                    "name": "test",
                                    "mountPath": "/test"
                                  }
                                ]
                              }
                            ]
                          }
                        }
                        """)
                .when()
                .put("/pod/{namespace}/{podName}", mockServer.getClient().getNamespace(), "serialization-test")
                .then()
                .statusCode(200);
        // JSON data is persisted into the mock server
        assertThat(mockServer.getClient().pods().withName("serialization-test").get())
                .hasFieldOrPropertyWithValue("metadata.name", "serialization-test")
                .extracting("spec.containers").asInstanceOf(InstanceOfAssertFactories.list(Container.class))
                .singleElement()
                .hasFieldOrPropertyWithValue("name", "test")
                .hasFieldOrPropertyWithValue("image", "busybox")
                .extracting(Container::getVolumeMounts)
                .asInstanceOf(InstanceOfAssertFactories.list(VolumeMount.class))
                .singleElement()
                .hasFieldOrPropertyWithValue("name", "test")
                .hasFieldOrPropertyWithValue("mountPath", "/test");
    }

    @Test
    @DisplayName("GET - Should deserialize the Pod into valid JSON")
    void deserialization() {
        mockServer.getClient().pods().resource(new PodBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("deserialization-test")
                        .build())
                .withSpec(new PodSpecBuilder()
                        .addToContainers(new ContainerBuilder()
                                .withName("deserialization-test-container")
                                .addToPorts(new ContainerPortBuilder()
                                        .withContainerPort(8080)
                                        .build())
                                .build())
                        .build())
                .build())
                .create();
        // JSON data is retrieved from the mock server
        when()
                .get("/pod/{namespace}/{podName}", mockServer.getClient().getNamespace(), "deserialization-test")
                .then()
                .statusCode(200)
                .body(
                        "metadata.name", is("deserialization-test"),
                        "spec.containers[0].name", is("deserialization-test-container"),
                        "spec.containers[0].ports[0].containerPort", is(8080),
                        "metadata.annotations", nullValue(),
                        // https://github.com/quarkusio/quarkus/issues/39934
                        "spec.overhead", nullValue())
                .log().ifValidationFails(LogDetail.BODY);
    }

    public static final class CrudEnvironmentPreparation implements Consumer<KubernetesServer> {

        @Override
        public void accept(KubernetesServer kubernetesServer) {
            final KubernetesClient kc = kubernetesServer.getClient();
            kc.configMaps().resource(new ConfigMapBuilder()
                    .withNewMetadata().withName("cmap1").endMetadata()
                    .addToData("dummy", "I'm required")
                    .build()).create();
            kc.configMaps().resource(new ConfigMapBuilder()
                    .withNewMetadata().withName("cmap2").endMetadata()
                    .addToData("dummysecret", "dumb")
                    .addToData("overridden.secret", "Alex")
                    .addToData("some.prop1", "I'm required")
                    .addToData("some.prop2", "I'm required (2)")
                    .addToData("some.prop3", "I'm required (3)")
                    .addToData("some.prop4", "I'm required (4)")
                    .addToData("some.prop5", "I'm required (5)")
                    .build()).create();
            kc.secrets().resource(new SecretBuilder()
                    .withNewMetadata().withName("s1").endMetadata()
                    .addToData("secret.prop1", "c2VjcmV0")
                    .addToData("secret.prop2", "c2VjcmV0")
                    .addToData("secret.prop3", "c2VjcmV0")
                    .addToData("secret.prop4", "c2VjcmV0")
                    .build()).create();
        }
    }
}
