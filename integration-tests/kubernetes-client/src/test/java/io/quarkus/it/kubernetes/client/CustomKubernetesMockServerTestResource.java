package io.quarkus.it.kubernetes.client;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.quarkus.test.kubernetes.client.KubernetesMockServerTestResource;

public class CustomKubernetesMockServerTestResource extends KubernetesMockServerTestResource {

    // setup the ConfigMap objects that the application expects to lookup configuration from
    @Override
    public void configureMockServer(KubernetesMockServer mockServer) {
        mockServer.expect().get().withPath("/api/v1/namespaces/test/configmaps/cmap1")
                .andReturn(200, configMapBuilder("cmap1")
                        .addToData("dummy", "dummy")
                        .addToData("some.prop1", "val1")
                        .addToData("some.prop2", "val2")
                        .addToData("application.properties", "some.prop3=val3")
                        .addToData("application.yaml", "some:\n  prop4: val4").build())
                .once();

        mockServer.expect().get().withPath("/api/v1/namespaces/test/configmaps/cmap2")
                .andReturn(200, configMapBuilder("cmap2")
                        .addToData("application.yaml", "some:\n  prop4: val4").build())
                .once();
    }

    private ConfigMapBuilder configMapBuilder(String name) {
        return new ConfigMapBuilder().withNewMetadata()
                .withName(name).endMetadata();
    }

}
