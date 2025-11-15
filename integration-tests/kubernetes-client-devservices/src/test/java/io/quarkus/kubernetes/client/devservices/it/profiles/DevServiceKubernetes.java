package io.quarkus.kubernetes.client.devservices.it.profiles;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class DevServiceKubernetes implements QuarkusTestProfile {

    public static final String API_VERSION = "1.24.1";

    @Override
    public Map<String, String> getConfigOverrides() {

        return Map.of("quarkus.kubernetes-client.devservices.api-version", API_VERSION,
                "quarkus.kubernetes-client.devservices.image-name",
                "quay.io/giantswarm/kube-apiserver:v%s".formatted(API_VERSION));
    }
}
