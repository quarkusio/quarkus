package io.quarkus.it.kubernetes.client;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodListBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesMockServerTestResource;
import io.quarkus.test.kubernetes.client.MockServer;
import io.restassured.RestAssured;

/*
 * KubernetesClientTest.TestResource contains the entire process of setting up the Mock Kubernetes API Server
 * It has to live there otherwise the Kubernetes client in native mode won't be able to locate the mock API Server
 */
@QuarkusTestResource(KubernetesMockServerTestResource.class)
@QuarkusTest
public class KubernetesClientTest {

    @MockServer
    private KubernetesMockServer mockServer;

    @Test
    public void before() {
        Pod pod1 = new PodBuilder().withNewMetadata().withName("pod1").withNamespace("test").and().build();
        Pod pod2 = new PodBuilder().withNewMetadata().withName("pod2").withNamespace("test").and().build();

        mockServer.expect().get().withPath("/api/v1/namespaces/test/pods")
                .andReturn(200,
                        new PodListBuilder().withNewMetadata().withResourceVersion("1").endMetadata().withItems(pod1, pod2)
                                .build())
                .always();

        mockServer.expect().get().withPath("/api/v1/namespaces/test/pods/pod1")
                .andReturn(200, pod1)
                .always();

        mockServer.expect().delete().withPath("/api/v1/namespaces/test/pods/pod1")
                .andReturn(200, "{}")
                .once();

        // it doesn't really matter what we return here, we just need to return a Pod to make sure
        // deserialization works
        mockServer.expect().put().withPath("/api/v1/namespaces/test/pods/pod1").andReturn(200, new PodBuilder()
                .withNewMetadata().withName("pod1").addToLabels("key1", "value1").endMetadata().build()).once();

        // same here, the content itself doesn't really matter
        mockServer.expect().post().withPath("/api/v1/namespaces/test/pods").andReturn(201, new PodBuilder()
                .withNewMetadata().withResourceVersion("12345").and().build()).once();
    }

    @Test
    public void testInteractionWithAPIServer() {
        RestAssured.when().get("/pod/test").then()
                .body("size()", is(2));

        RestAssured.when().delete("/pod/test").then()
                .statusCode(204);

        RestAssured.when().put("/pod/test").then()
                .body(containsString("value1"));

        RestAssured.when().post("/pod/test").then()
                .body(containsString("12345"));
    }

}
