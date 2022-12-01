package io.quarkus.it.kubernetes.client;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.restassured.RestAssured;

/*
 * KubernetesClientTest.TestResource contains the entire process of setting up the Mock Kubernetes API Server
 * It has to live there otherwise the Kubernetes client in native mode won't be able to locate the mock API Server
 */
@QuarkusTestResource(value = CustomKubernetesTestServerTestResource.class, restrictToAnnotatedClass = true)
@QuarkusTest
class KubernetesNewClientTest {

    @KubernetesTestServer
    private KubernetesServer mockServer;

    @Test
    void testInteractionWithAPIServer() {
        setupMockServerForTest();

        RestAssured.when().get("/pod/test").then()
                .body("size()", is(2)).body(containsString("pod1"), containsString("pod2"));

        RestAssured.when().delete("/pod/test").then()
                .statusCode(204);

        RestAssured.when().put("/pod/test").then()
                .body(containsString("value1"));

        RestAssured.when().post("/pod/test").then()
                .body(containsString("12345"));
    }

    private void setupMockServerForTest() {
        Pod pod1 = new PodBuilder().withNewMetadata().withName("pod1").withNamespace("test").and().build();
        Pod pod2 = new PodBuilder().withNewMetadata().withName("pod2").withNamespace("test").and().build();

        mockServer.getClient().inNamespace("test").pods().create(pod1);
        mockServer.getClient().inNamespace("test").pods().create(pod2);
    }

}
