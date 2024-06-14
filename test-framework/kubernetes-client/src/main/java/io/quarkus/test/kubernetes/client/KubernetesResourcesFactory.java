package io.quarkus.test.kubernetes.client;

import java.util.Collections;
import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;

public interface KubernetesResourcesFactory {
    @SuppressWarnings("unused")
    default List<? extends HasMetadata> build(String namespace) {
        return Collections.emptyList();
    }
}
