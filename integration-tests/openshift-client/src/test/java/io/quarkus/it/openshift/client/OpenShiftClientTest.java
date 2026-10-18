package io.quarkus.it.openshift.client;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.PodListBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesServer;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.restassured.RestAssured;

@WithKubernetesTestServer
@QuarkusTest
public class OpenShiftClientTest {

    @KubernetesTestServer
    KubernetesServer mockServer;

    @Test
    void createRoute() {

        mockServer.expect().post().withPath("/apis/route.openshift.io/v1/namespaces/test/routes")
                .andReturn(200, new PodListBuilder().build())
                .once();

        Route expectedRoute = new RouteBuilder().withNewMetadata().withName("myroute").withNamespace("test").endMetadata()
                .build();
        mockServer.expect().get().withPath("/apis/route.openshift.io/v1/namespaces/test/routes/myroute")
                .andReturn(200, expectedRoute)
                .once();

        NamespacedOpenShiftClient openShiftClient = mockServer.getClient().adapt(NamespacedOpenShiftClient.class);
        openShiftClient.routes()
                .resource(new RouteBuilder().withNewMetadata().withName("myroute").withNamespace("test").endMetadata().build())
                .create();
        Route route = openShiftClient.routes().inNamespace("test").withName("myroute").get();
        Assertions.assertNotNull(route);
    }

    @Test
    void getRoutes() {

        mockServer.expect().get().withPath("/apis/route.openshift.io/v1/namespaces/test/routes")
                .andReturn(200, new PodListBuilder().build())
                .once();

        RestAssured.when().get("/route/test").then()
                .body("size()", is(0));
    }
}
