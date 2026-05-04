package io.quarkus.opentelemetry.runtime.resource;

import static io.opentelemetry.semconv.incubating.K8sIncubatingAttributes.K8S_CLUSTER_NAME;
import static io.opentelemetry.semconv.incubating.K8sIncubatingAttributes.K8S_CONTAINER_NAME;
import static io.opentelemetry.semconv.incubating.K8sIncubatingAttributes.K8S_DEPLOYMENT_NAME;
import static io.opentelemetry.semconv.incubating.K8sIncubatingAttributes.K8S_NAMESPACE_NAME;
import static io.opentelemetry.semconv.incubating.K8sIncubatingAttributes.K8S_NODE_NAME;
import static io.opentelemetry.semconv.incubating.K8sIncubatingAttributes.K8S_POD_NAME;
import static io.opentelemetry.semconv.incubating.K8sIncubatingAttributes.K8S_POD_UID;

import java.nio.file.Files;
import java.nio.file.Path;

import org.jboss.logging.Logger;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;

public class KubernetesResourceProvider {

    private static final Logger log = Logger.getLogger(KubernetesResourceProvider.class);

    public static final String NAMESPACE_ENV = "QUARKUS_OTEL_K8S_RESOURCE_NAMESPACE";
    public static final String POD_NAME_ENV = "QUARKUS_OTEL_K8S_RESOURCE_POD_NAME";
    public static final String POD_UID_ENV = "QUARKUS_OTEL_K8S_RESOURCE_POD_UID";
    public static final String NODE_NAME_ENV = "QUARKUS_OTEL_K8S_RESOURCE_NODE_NAME";
    public static final String CONTAINER_NAME_ENV = "QUARKUS_OTEL_K8S_RESOURCE_CONTAINER_NAME";
    public static final String DEPLOYMENT_NAME_ENV = "QUARKUS_OTEL_K8S_RESOURCE_DEPLOYMENT_NAME";
    public static final String CLUSTER_NAME_ENV = "QUARKUS_OTEL_K8S_RESOURCE_CLUSTER_NAME";

    private static final String NAMESPACE_FILE = "/var/run/secrets/kubernetes.io/serviceaccount/namespace";

    private KubernetesResourceProvider() {
    }

    public static Resource createResource() {
        AttributesBuilder builder = Attributes.builder();

        // Namespace: from env var or filesystem fallback
        String namespace = getEnv(NAMESPACE_ENV);
        if (namespace == null) {
            namespace = readNamespaceFromFile(NAMESPACE_FILE);
        }
        if (namespace != null) {
            builder.put(K8S_NAMESPACE_NAME, namespace);
        }

        addIfSet(builder, K8S_POD_NAME, getEnv(POD_NAME_ENV));
        addIfSet(builder, K8S_POD_UID, getEnv(POD_UID_ENV));
        addIfSet(builder, K8S_NODE_NAME, getEnv(NODE_NAME_ENV));
        addIfSet(builder, K8S_CONTAINER_NAME, getEnv(CONTAINER_NAME_ENV));
        addIfSet(builder, K8S_DEPLOYMENT_NAME, getEnv(DEPLOYMENT_NAME_ENV));
        addIfSet(builder, K8S_CLUSTER_NAME, getEnv(CLUSTER_NAME_ENV));

        return Resource.create(builder.build());
    }

    private static String getEnv(String name) {
        String value = System.getenv(name);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return null;
    }

    private static void addIfSet(AttributesBuilder builder,
            AttributeKey<String> key, String value) {
        if (value != null) {
            builder.put(key, value);
        }
    }

    private static String readNamespaceFromFile(String path) {
        try {
            Path namespacePath = Path.of(path);
            if (Files.exists(namespacePath) && Files.isReadable(namespacePath)) {
                return Files.readString(namespacePath).trim();
            }
        } catch (Exception e) {
            log.debug("Failed to read Kubernetes namespace from file: " + path, e);
        }
        return null;
    }
}
