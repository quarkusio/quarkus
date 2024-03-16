package io.quarkus.oidc.client.graphql;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.HashMap;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.oidc.client.graphql.runtime.OidcClientGraphQLConfig;
import io.quarkus.oidc.client.graphql.runtime.OidcGraphQLClientIntegrationRecorder;
import io.quarkus.smallrye.graphql.client.deployment.GraphQLClientConfigInitializedBuildItem;

public class OidcGraphQLClientIntegrationProcessor {

    private static final DotName GRAPHQL_CLIENT_API = DotName
            .createSimple("io.smallrye.graphql.client.typesafe.api.GraphQLClientApi");

    private static final String OIDC_CLIENT_FILTER = "io.quarkus.oidc.client.filter.OidcClientFilter";

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> featureProducer) {
        featureProducer.produce(new FeatureBuildItem(Feature.OIDC_CLIENT_GRAPHQL_CLIENT_INTEGRATION));
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void initialize(BeanContainerBuildItem containerBuildItem,
            OidcGraphQLClientIntegrationRecorder recorder,
            OidcClientGraphQLConfig config,
            BeanArchiveIndexBuildItem index,
            GraphQLClientConfigInitializedBuildItem configInitialized) {
        Map<String, String> configKeysToOidcClients = new HashMap<>();
        for (AnnotationInstance annotation : index.getIndex().getAnnotations(GRAPHQL_CLIENT_API)) {
            ClassInfo clazz = annotation.target().asClass();
            AnnotationInstance oidcClient = clazz.annotation(OIDC_CLIENT_FILTER);
            if (oidcClient != null) {
                String oidcClientName = oidcClient.valueWithDefault(index.getIndex(), "value").asString();
                AnnotationValue configKeyValue = annotation.value("configKey");
                String configKey = configKeyValue != null ? configKeyValue.asString() : null;
                String actualConfigKey = (configKey != null && !configKey.equals("")) ? configKey : clazz.name().toString();
                if (oidcClientName != null && !oidcClientName.isEmpty()) {
                    configKeysToOidcClients.put(actualConfigKey, oidcClientName);
                }
            }
        }
        recorder.enhanceGraphQLClientConfigurationWithOidc(configKeysToOidcClients, config.clientName().orElse(null));
    }
}
