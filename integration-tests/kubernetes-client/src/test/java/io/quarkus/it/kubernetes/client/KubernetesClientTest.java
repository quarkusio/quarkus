package io.quarkus.it.kubernetes.client;

import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.interfaces.ECPrivateKey;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodListBuilder;
import io.fabric8.kubernetes.client.internal.CertUtils;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.MockServer;
import io.restassured.RestAssured;

/*
 * KubernetesClientTest.TestResource contains the entire process of setting up the Mock Kubernetes API Server
 * It has to live there otherwise the Kubernetes client in native mode won't be able to locate the mock API Server
 */
@QuarkusTestResource(value = CustomKubernetesMockServerTestResource.class, restrictToAnnotatedClass = true)
@QuarkusTest
public class KubernetesClientTest {

    @MockServer
    private KubernetesMockServer mockServer;

    @Test
    public void testInteractionWithAPIServer() {
        setupMockServerForTest();

        RestAssured.when().get("/pod/test").then()
                .body("size()", is(2)).body(containsString("pod1"), containsString("pod2"));

        RestAssured.when().delete("/pod/test").then()
                .statusCode(204);

        RestAssured.when().put("/pod/test").then()
                .body(containsString("value1"));

        RestAssured.when().post("/pod/test").then()
                .body(containsString("54321"));

        RestAssured.when().get("/version").then()
                .statusCode(200);
    }

    @Test
    public void testEcKeyIsSupported() throws Exception {
        InputStream certInputStream = KubernetesClientTest.class.getResourceAsStream("/cert.pem");
        InputStream keyInputStream = KubernetesClientTest.class.getResourceAsStream("/private-key.pem");

        try {
            KeyStore keyStore = CertUtils.createKeyStore(certInputStream, keyInputStream, "EC", "eckey".toCharArray(),
                    (String) null, "keystore".toCharArray());
            Key key = keyStore.getKey("CN=Client,OU=Test,O=Test", "eckey".toCharArray());
            assertTrue(key instanceof ECPrivateKey);
        } finally {
            certInputStream.close();
            keyInputStream.close();
        }
    }

    private void setupMockServerForTest() {
        Pod pod1 = new PodBuilder().withNewMetadata().withName("pod1").withNamespace("test").and().build();
        Pod pod2 = new PodBuilder().withNewMetadata().withName("pod2").withNamespace("test").and().build();

        /*
         * We take special care here to only create as many mock responses as this specific test class needs
         * This is done in order to avoid leaking mock responses into other tests
         */

        mockServer.expect().get().withPath("/api/v1/namespaces/test/pods")
                .andReturn(200,
                        new PodListBuilder().withNewMetadata().withResourceVersion("1").endMetadata().withItems(pod1, pod2)
                                .build())
                // GET /pod/test,
                // DELETE /pod/test,
                // PUT /pod/test
                // all list the pods
                .times(3);

        mockServer.expect().get().withPath("/api/v1/namespaces/test/pods/pod1")
                .andReturn(200, pod1)
                // DELETE /pod/test,
                // PUT /pod/test
                // both look at this endpoint to see if the pod exists
                .times(2);

        mockServer.expect().delete().withPath("/api/v1/namespaces/test/pods/pod1")
                .andReturn(200, new PodBuilder(pod1)
                        .editMetadata().withDeletionTimestamp(Instant.now().toString()).endMetadata().build())
                .once();

        // PUT on /pod/test will createOrReplace, which attempts a POST first, then a PUT if receiving a 409
        mockServer.expect().post().withPath("/api/v1/namespaces/test/pods")
                .andReturn(HTTP_CONFLICT, "{}")
                .once();

        // it doesn't really matter what we return here, we just need to return a Pod to make sure
        // deserialization works
        mockServer.expect().put().withPath("/api/v1/namespaces/test/pods/pod1").andReturn(200, new PodBuilder()
                .withNewMetadata().withName("pod1").addToLabels("key1", "value1").endMetadata().build()).once();

        // same here, the content itself doesn't really matter
        mockServer.expect().post().withPath("/api/v1/namespaces/test/pods").andReturn(201, new PodBuilder()
                .withNewMetadata().withResourceVersion("54321").and().build()).once();
    }

}
