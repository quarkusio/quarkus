package io.quarkus.it.kubernetes.client;

import java.util.Base64;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.quarkus.test.kubernetes.client.KubernetesMockServerTestResource;

public class CustomKubernetesMockServerTestResource extends KubernetesMockServerTestResource {

    // setup the ConfigMap objects that the application expects to lookup configuration from
    @Override
    public void configureMockServer(KubernetesMockServer mockServer) {
        mockServer.expect().get().withPath("/api/v1/namespaces/test/configmaps/cmap1")
                .andReturn(200, configMapBuilder("cmap1")
                        .addToData("dummy", "dummy")
                        .addToData("overridden.secret", "cm") // will be overridden since secrets have a higher priority
                        .addToData("some.prop1", "val1")
                        .addToData("some.prop2", "val2")
                        .addToData("some.prop4", "v4") // will be overridden since cmap2 has a higher priority
                        .addToData("some.prop5", "val5")
                        .addToData("application.properties", "some.prop3=val3")
                        .addToData("application.yaml", "some:\n  prop4: val4").build())
                .once();

        mockServer.expect().get().withPath("/api/v1/namespaces/test/configmaps/cmap2")
                .andReturn(200, configMapBuilder("cmap2")
                        .addToData("application.yaml", "some:\n  prop4: val4").build())
                .once();

        mockServer.expect().get().withPath("/api/v1/namespaces/demo/configmaps/cmap3")
                .andReturn(200, configMapBuilder("cmap3")
                        .addToData("dummy", "dummyFromDemo")
                        .addToData("some.prop1", "val1FromDemo")
                        .addToData("some.prop2", "val2FromDemo")
                        .addToData("some.prop5", "val5FromDemo")
                        .addToData("application.properties", "some.prop3=val3FromDemo")
                        .addToData("application.yaml", "some:\n  prop4: val4FromDemo").build())
                .once();

        mockServer.expect().get().withPath("/api/v1/namespaces/test/secrets/s1")
                .andReturn(200, secretBuilder("s1")
                        .addToData("dummysecret", encodeValue("dummysecret"))
                        .addToData("overridden.secret", encodeValue("secret"))
                        .addToData("secret.prop1", encodeValue("val1"))
                        .addToData("secret.prop2", encodeValue("val2"))
                        .addToData("application.properties", encodeValue("secret.prop3=val3"))
                        .addToData("application.yaml", encodeValue("secret:\n  prop4: val4")).build())
                .once();

        mockServer.expect().get().withPath("/api/v1/namespaces/demo/secrets/s1")
                .andReturn(200, secretBuilder("s1")
                        .addToData("dummysecret", encodeValue("dummysecretFromDemo"))
                        .addToData("overridden.secret", encodeValue("secretFromDemo"))
                        .addToData("secret.prop1", encodeValue("val1FromDemo"))
                        .addToData("secret.prop2", encodeValue("val2FromDemo"))
                        .addToData("application.properties", encodeValue("secret.prop3=val3FromDemo"))
                        .addToData("application.yaml", encodeValue("secret:\n  prop4: val4FromDemo")).build())
                .once();
    }

    private ConfigMapBuilder configMapBuilder(String name) {
        return new ConfigMapBuilder().withNewMetadata()
                .withName(name).endMetadata();
    }

    private SecretBuilder secretBuilder(String name) {
        return new SecretBuilder().withNewMetadata()
                .withName(name).endMetadata();
    }

    private String encodeValue(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes());
    }
}
