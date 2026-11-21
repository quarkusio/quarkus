package io.quarkus.oidc.token.propagation.graphql.deployment;

import java.util.ArrayDeque;
import java.util.Deque;
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
import org.jboss.jandex.MethodInfo;

import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.oidc.client.filter.OidcClientFilter;
import io.quarkus.oidc.token.propagation.common.deployment.AccessTokenInstanceBuildItem;
import io.quarkus.oidc.token.propagation.graphql.runtime.GraphQLAccessTokenProvider;
import io.quarkus.smallrye.graphql.client.deployment.GraphQLClientAuthorizationHeaderBuildItem;
import io.quarkus.smallrye.graphql.client.deployment.GraphQLClientConfigInitializedBuildItem;

class OidcTokenPropagationGraphQLProcessor {

    private static final DotName GRAPHQL_CLIENT_API = DotName
            .createSimple("io.smallrye.graphql.client.typesafe.api.GraphQLClientApi");

    private static final DotName OIDC_CLIENT_FILTER = DotName.createSimple(OidcClientFilter.class);

    private static final DotName MULTI = DotName.createSimple("io.smallrye.mutiny.Multi");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.OIDC_TOKEN_PROPAGATION_GRAPHQL);
    }

    @BuildStep(onlyIf = IsEnabled.class)
    void propagateAccessTokens(List<AccessTokenInstanceBuildItem> accessTokenInstances,
            CombinedIndexBuildItem combinedIndex,
            ServiceRegistrar serviceRegistrar,
            BuildProducer<GraphQLClientAuthorizationHeaderBuildItem> authorizationHeaderProducer) {
        IndexView index = combinedIndex.getIndex();
        Map<DotName, AccessTokenInstanceBuildItem> graphQLClientToAccessToken = collectGraphQLClientAccessTokens(
                accessTokenInstances, index);
        if (graphQLClientToAccessToken.isEmpty()) {
            return;
        }

        // the GraphQL clients with the same configuration key share the Authorization header
        Map<String, AccessTokenInstanceBuildItem> configKeyToAccessToken = new HashMap<>();
        for (AccessTokenInstanceBuildItem accessToken : graphQLClientToAccessToken.values()) {
            configKeyToAccessToken.putIfAbsent(resolveConfigKey(accessToken.getAnnotationTarget().asClass()), accessToken);
        }
        validateGraphQLClients(graphQLClientToAccessToken, configKeyToAccessToken, index);

        Map<String, String> configKeyToExchangeOidcClient = new HashMap<>();
        for (Map.Entry<String, AccessTokenInstanceBuildItem> configKeyAndAccessToken : configKeyToAccessToken.entrySet()) {
            String configKey = configKeyAndAccessToken.getKey();
            authorizationHeaderProducer
                    .produce(new GraphQLClientAuthorizationHeaderBuildItem(configKey, "the @AccessToken annotation"));
            if (configKeyAndAccessToken.getValue().exchangeTokenActivated()) {
                configKeyToExchangeOidcClient.put(configKey, configKeyAndAccessToken.getValue().getClientName());
            }
        }

        Set<String> propagateConfigKeys = Set.copyOf(configKeyToAccessToken.keySet());
        Map<String, String> exchangeOidcClients = Map.copyOf(configKeyToExchangeOidcClient);
        serviceRegistrar.forService("io.quarkus.oidc-token-propagation-graphql.propagate-access-tokens")
                .afterBuildItem(GraphQLClientConfigInitializedBuildItem.class)
                // GraphQL clients must propagate access tokens before the application starts using them
                .before("io.quarkus.arc.startup-event")
                .onStart(ctx -> GraphQLAccessTokenProvider.propagateAccessTokens(ctx, propagateConfigKeys,
                        exchangeOidcClients));
    }

    /**
     * @return the {@code @AccessToken} annotations of the GraphQL typesafe clients, keyed by the client interface name
     */
    private static Map<DotName, AccessTokenInstanceBuildItem> collectGraphQLClientAccessTokens(
            List<AccessTokenInstanceBuildItem> accessTokenInstances, IndexView index) {
        Map<DotName, AccessTokenInstanceBuildItem> graphQLClientToAccessToken = new HashMap<>();
        for (AccessTokenInstanceBuildItem instance : accessTokenInstances) {
            AnnotationTarget annotationTarget = instance.getAnnotationTarget();
            boolean methodLevel = annotationTarget.kind() == AnnotationTarget.Kind.METHOD;
            ClassInfo graphQLClientApi = methodLevel ? annotationTarget.asMethod().declaringClass()
                    : annotationTarget.asClass();
            if (!graphQLClientApi.hasDeclaredAnnotation(GRAPHQL_CLIENT_API)) {
                // GraphQL typesafe clients also have the methods of their parent interfaces
                for (ClassInfo subinterface : index.getAllKnownSubinterfaces(graphQLClientApi.name())) {
                    if (subinterface.hasDeclaredAnnotation(GRAPHQL_CLIENT_API)) {
                        throw new DeploymentException(
                                String.format("@AccessToken on the parent interface '%s' of GraphQL client '%s' "
                                        + "is not supported; annotate the client interface instead.",
                                        instance.targetClass(), subinterface.name()));
                    }
                }
                // not a GraphQL typesafe client; handled by the REST token-propagation modules
                continue;
            }
            if (methodLevel) {
                throw new DeploymentException(
                        String.format("@AccessToken on method '%s' of GraphQL client '%s' is not supported; "
                                + "annotate the client interface instead.",
                                annotationTarget.asMethod().name(), instance.targetClass()));
            }
            if (graphQLClientApi.hasDeclaredAnnotation(OIDC_CLIENT_FILTER)) {
                throw new DeploymentException(
                        String.format("GraphQL client '%s' is annotated with both @AccessToken and @OidcClientFilter; "
                                + "both mechanisms set the Authorization header, so only one of them can be used "
                                + "on the same client.",
                                instance.targetClass()));
            }
            graphQLClientToAccessToken.put(graphQLClientApi.name(), instance);
        }
        return graphQLClientToAccessToken;
    }

    /**
     * Validates all the GraphQL typesafe clients which propagate the access token, including the clients which are
     * not annotated with the {@code @AccessToken} annotation, but share the configuration key with an annotated client.
     */
    private static void validateGraphQLClients(Map<DotName, AccessTokenInstanceBuildItem> graphQLClientToAccessToken,
            Map<String, AccessTokenInstanceBuildItem> configKeyToAccessToken, IndexView index) {
        for (AnnotationInstance graphQLClientApiAnnotation : index.getAnnotations(GRAPHQL_CLIENT_API)) {
            ClassInfo graphQLClientApi = graphQLClientApiAnnotation.target().asClass();
            String configKey = resolveConfigKey(graphQLClientApi);
            AccessTokenInstanceBuildItem expectedAccessToken = configKeyToAccessToken.get(configKey);
            if (expectedAccessToken == null) {
                continue;
            }
            AccessTokenInstanceBuildItem accessToken = graphQLClientToAccessToken.get(graphQLClientApi.name());
            if (accessToken == null || accessToken.exchangeTokenActivated() != expectedAccessToken.exchangeTokenActivated()
                    || !accessToken.getClientName().equals(expectedAccessToken.getClientName())) {
                throw new DeploymentException(
                        String.format("GraphQL clients '%s' and '%s' share the configuration key '%s', therefore both "
                                + "must be annotated with @AccessToken with the same 'exchangeTokenClient' attribute value.",
                                expectedAccessToken.targetClass(), graphQLClientApi.name(), configKey));
            }
            MethodInfo subscription = findSubscription(graphQLClientApi, index);
            if (subscription != null) {
                throw new DeploymentException(
                        String.format("GraphQL client '%s' is annotated with @AccessToken, but its method '%s' is "
                                + "a subscription and the access token cannot be propagated over WebSocket; declare "
                                + "the subscriptions in a GraphQL client with a different configuration key, which is "
                                + "not annotated with @AccessToken.",
                                graphQLClientApi.name(), subscription.name()));
            }
        }
    }

    private static String resolveConfigKey(ClassInfo graphQLClientApi) {
        AnnotationValue configKeyValue = graphQLClientApi.declaredAnnotation(GRAPHQL_CLIENT_API).value("configKey");
        String configKey = configKeyValue != null ? configKeyValue.asString() : null;
        return (configKey != null && !configKey.isEmpty()) ? configKey : graphQLClientApi.name().toString();
    }

    /**
     * @return a method of the GraphQL client, or of one of its parent interfaces, which is always executed over WebSocket
     */
    private static MethodInfo findSubscription(ClassInfo graphQLClientApi, IndexView index) {
        Set<DotName> visitedInterfaces = new HashSet<>();
        Deque<ClassInfo> interfaces = new ArrayDeque<>();
        interfaces.add(graphQLClientApi);
        while (!interfaces.isEmpty()) {
            ClassInfo currentInterface = interfaces.poll();
            if (!visitedInterfaces.add(currentInterface.name())) {
                continue;
            }
            for (MethodInfo method : currentInterface.methods()) {
                if (MULTI.equals(method.returnType().name())) {
                    return method;
                }
            }
            for (DotName parentInterfaceName : currentInterface.interfaceNames()) {
                ClassInfo parentInterface = index.getClassByName(parentInterfaceName);
                if (parentInterface != null) {
                    interfaces.add(parentInterface);
                }
            }
        }
        return null;
    }

    static class IsEnabled implements BooleanSupplier {
        OidcTokenPropagationGraphQLBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled();
        }
    }
}
