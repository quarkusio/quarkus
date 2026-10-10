package io.quarkus.oidc.token.propagation.graphql.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.oidc.client.filter.OidcClientFilter;
import io.quarkus.oidc.token.propagation.common.deployment.AccessTokenInstanceBuildItem;
import io.quarkus.oidc.token.propagation.graphql.runtime.OidcTokenPropagationGraphQLRecorder;
import io.quarkus.smallrye.graphql.client.deployment.GraphQLClientConfigInitializedBuildItem;

class OidcTokenPropagationGraphQLProcessor {

    private static final DotName GRAPHQL_CLIENT_API = DotName
            .createSimple("io.smallrye.graphql.client.typesafe.api.GraphQLClientApi");

    private static final DotName OIDC_CLIENT_FILTER = DotName.createSimple(OidcClientFilter.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SMALLRYE_GRAPHQL_CLIENT_OIDC_TOKEN_PROPAGATION);
    }

    @BuildStep(onlyIf = IsEnabled.class)
    @Record(RUNTIME_INIT)
    @Consume(GraphQLClientConfigInitializedBuildItem.class)
    void propagateAccessTokens(List<AccessTokenInstanceBuildItem> accessTokenInstances,
            BeanArchiveIndexBuildItem indexBuildItem,
            OidcTokenPropagationGraphQLRecorder recorder) {
        IndexView index = indexBuildItem.getIndex();

        Set<String> propagateConfigKeys = new HashSet<>();
        Map<String, String> configKeyToExchangeOidcClient = new HashMap<>();

        for (AccessTokenInstanceBuildItem instance : accessTokenInstances) {
            ClassInfo graphQLClientApi = index.getClassByName(DotName.createSimple(instance.targetClass()));
            if (graphQLClientApi == null || !graphQLClientApi.hasDeclaredAnnotation(GRAPHQL_CLIENT_API)) {
                // not a GraphQL typesafe client; handled by the REST token-propagation modules
                continue;
            }
            if (instance.getAnnotationTarget().kind() == AnnotationTarget.Kind.METHOD) {
                throw new DeploymentException(
                        String.format("@AccessToken on method '%s' of GraphQL client '%s' is not supported; "
                                + "annotate the client interface instead.",
                                instance.getAnnotationTarget().asMethod().name(), instance.targetClass()));
            }
            if (graphQLClientApi.hasDeclaredAnnotation(OIDC_CLIENT_FILTER)) {
                throw new DeploymentException(
                        String.format("GraphQL client '%s' is annotated with both @AccessToken and @OidcClientFilter; "
                                + "both mechanisms set the Authorization header, so only one of them can be used "
                                + "on the same client.",
                                instance.targetClass()));
            }
            String configKey = resolveConfigKey(graphQLClientApi);
            propagateConfigKeys.add(configKey);
            if (instance.exchangeTokenActivated()) {
                configKeyToExchangeOidcClient.put(configKey, instance.getClientName());
            }
        }

        if (!propagateConfigKeys.isEmpty()) {
            recorder.enhanceGraphQLClients(propagateConfigKeys, configKeyToExchangeOidcClient);
        }
    }

    private static String resolveConfigKey(ClassInfo graphQLClientApi) {
        AnnotationInstance annotation = graphQLClientApi.declaredAnnotation(GRAPHQL_CLIENT_API);
        AnnotationValue configKeyValue = annotation != null ? annotation.value("configKey") : null;
        String configKey = configKeyValue != null ? configKeyValue.asString() : null;
        return (configKey != null && !configKey.isEmpty()) ? configKey : graphQLClientApi.name().toString();
    }

    static class IsEnabled implements BooleanSupplier {
        OidcTokenPropagationGraphQLBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled();
        }
    }
}
