package io.quarkus.oidc.token.propagation.graphql.deployment;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.oidc.client.filter.OidcClientFilter;
import io.quarkus.oidc.token.propagation.common.deployment.AccessTokenInstanceBuildItem;
import io.quarkus.oidc.token.propagation.graphql.deployment.spi.GraphQLClientTokenPropagationBuildItem;
import io.quarkus.oidc.token.propagation.graphql.runtime.internal.GraphQLAccessTokenProvider;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.smallrye.graphql.client.deployment.GraphQLClientConfigInitializedBuildItem;
import io.smallrye.graphql.client.typesafe.api.AuthorizationHeader;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.smallrye.graphql.client.typesafe.api.Header;
import io.smallrye.graphql.client.typesafe.api.Headers;

class OidcTokenPropagationGraphQLProcessor {

    private static final DotName GRAPHQL_CLIENT_API = DotName.createSimple(GraphQLClientApi.class);

    private static final DotName OIDC_CLIENT_FILTER = DotName.createSimple(OidcClientFilter.class);

    private static final DotName AUTHORIZATION_HEADER = DotName.createSimple(AuthorizationHeader.class);

    private static final DotName HEADER = DotName.createSimple(Header.class);

    private static final DotName HEADERS = DotName.createSimple(Headers.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.OIDC_TOKEN_PROPAGATION_GRAPHQL);
    }

    @BuildStep(onlyIf = IsEnabled.class)
    void propagateAccessTokens(List<AccessTokenInstanceBuildItem> accessTokenInstances,
            CombinedIndexBuildItem combinedIndex,
            ServiceRegistrar serviceRegistrar,
            BuildProducer<GraphQLClientTokenPropagationBuildItem> tokenPropagationProducer) {
        IndexView index = combinedIndex.getIndex();
        Map<DotName, AccessTokenInstanceBuildItem> graphQLClientToAccessToken = collectGraphQLClientAccessTokens(
                accessTokenInstances, index);
        if (graphQLClientToAccessToken.isEmpty()) {
            return;
        }

        // the GraphQL clients with the same configuration key share the Authorization header
        Map<String, AccessTokenInstanceBuildItem> configKeyToAccessToken = new TreeMap<>();
        for (AccessTokenInstanceBuildItem accessToken : graphQLClientToAccessToken.values()) {
            configKeyToAccessToken.putIfAbsent(resolveConfigKey(accessToken.getAnnotationTarget().asClass()), accessToken);
        }
        validateGraphQLClients(graphQLClientToAccessToken, configKeyToAccessToken, index);

        Map<String, String> configKeyToExchangeOidcClient = new HashMap<>();
        for (Map.Entry<String, AccessTokenInstanceBuildItem> configKeyAndAccessToken : configKeyToAccessToken.entrySet()) {
            String configKey = configKeyAndAccessToken.getKey();
            tokenPropagationProducer.produce(new GraphQLClientTokenPropagationBuildItem(configKey));
            if (configKeyAndAccessToken.getValue().exchangeTokenActivated()) {
                configKeyToExchangeOidcClient.put(configKey, configKeyAndAccessToken.getValue().getClientName());
            }
        }

        Set<String> propagateConfigKeys = Set.copyOf(configKeyToAccessToken.keySet());
        Map<String, String> exchangeOidcClients = Map.copyOf(configKeyToExchangeOidcClient);
        serviceRegistrar.forService("io.quarkus.oidc-token-propagation-graphql.propagate-access-tokens")
                .require(ArcContainer.class)
                .afterBuildItem(GraphQLClientConfigInitializedBuildItem.class)
                // the OIDC clients which exchange the access tokens must be created first
                .after("io.quarkus.oidc-client.init-clients")
                // GraphQL clients must propagate access tokens before the application starts using them
                .before("io.quarkus.arc.startup-event")
                .onStart((ctx, container) -> GraphQLAccessTokenProvider.propagateAccessTokens(ctx, container,
                        propagateConfigKeys, exchangeOidcClients));
    }

    /**
     * @return the {@code @AccessToken} annotations of the GraphQL typesafe clients, keyed by the client interface name
     */
    private static Map<DotName, AccessTokenInstanceBuildItem> collectGraphQLClientAccessTokens(
            List<AccessTokenInstanceBuildItem> accessTokenInstances, IndexView index) {
        Map<DotName, AccessTokenInstanceBuildItem> graphQLClientToAccessToken = new TreeMap<>();
        for (AccessTokenInstanceBuildItem instance : accessTokenInstances) {
            AnnotationTarget annotationTarget = instance.getAnnotationTarget();
            boolean methodLevel = annotationTarget.kind() == AnnotationTarget.Kind.METHOD;
            ClassInfo graphQLClientApi = methodLevel ? annotationTarget.asMethod().declaringClass()
                    : annotationTarget.asClass();
            if (!graphQLClientApi.hasDeclaredAnnotation(GRAPHQL_CLIENT_API)) {
                // GraphQL typesafe clients also have the methods of their parent interfaces
                for (ClassInfo subinterface : index.getAllKnownSubinterfaces(graphQLClientApi.name())) {
                    if (subinterface.hasDeclaredAnnotation(GRAPHQL_CLIENT_API)) {
                        throw new ConfigurationException(
                                String.format("@AccessToken on the parent interface '%s' of GraphQL client '%s' "
                                        + "is not supported; annotate the client interface instead.",
                                        instance.targetClass(), subinterface.name()));
                    }
                }
                // not a GraphQL typesafe client; handled by the REST token-propagation modules
                continue;
            }
            if (methodLevel) {
                throw new ConfigurationException(
                        String.format("@AccessToken on method '%s' of GraphQL client '%s' is not supported; "
                                + "annotate the client interface instead.",
                                annotationTarget.asMethod().name(), instance.targetClass()));
            }
            if (graphQLClientApi.hasAnnotation(OIDC_CLIENT_FILTER)) {
                throw new ConfigurationException(
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
                throw new ConfigurationException(
                        String.format("GraphQL clients '%s' and '%s' share the configuration key '%s', so both must have "
                                + "the same @AccessToken annotation or use different configuration keys.",
                                expectedAccessToken.targetClass(), graphQLClientApi.name(), configKey));
            }
            // GraphQL typesafe clients also have the methods and the headers of their parent interfaces
            List<ClassInfo> interfaces = collectInterfaces(graphQLClientApi, index);
            validateNoAuthorizationHeader(graphQLClientApi, interfaces);
            MethodInfo subscription = findSubscription(interfaces);
            if (subscription != null) {
                throw new ConfigurationException(
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
     * GraphQL clients send the propagated access token together with the {@code Authorization} header they already have.
     */
    private static void validateNoAuthorizationHeader(ClassInfo graphQLClientApi, List<ClassInfo> interfaces) {
        for (ClassInfo currentInterface : interfaces) {
            String annotation = null;
            if (currentInterface.hasAnnotation(AUTHORIZATION_HEADER)) {
                annotation = "@AuthorizationHeader";
            } else {
                for (AnnotationInstance header : currentInterface.annotationsWithRepeatable(HEADER, HEADERS)) {
                    AnnotationValue name = header.value("name");
                    if (name != null && "Authorization".equalsIgnoreCase(name.asString())) {
                        annotation = "@Header";
                    }
                }
            }
            if (annotation != null) {
                throw new ConfigurationException(
                        String.format("GraphQL client '%s' is annotated with @AccessToken, but the %s annotation declared "
                                + "in '%s' also sets the Authorization header; both mechanisms set the Authorization header, "
                                + "so only one of them can be used on the same client.",
                                graphQLClientApi.name(), annotation, currentInterface.name()));
            }
        }
    }

    /**
     * @return a method of the GraphQL client, or of one of its parent interfaces, which is always executed over WebSocket
     */
    private static MethodInfo findSubscription(List<ClassInfo> interfaces) {
        for (ClassInfo currentInterface : interfaces) {
            for (MethodInfo method : currentInterface.methods()) {
                if (DotNames.MULTI.equals(method.returnType().name())) {
                    return method;
                }
            }
        }
        return null;
    }

    /**
     * @return the GraphQL client interface and its parent interfaces
     */
    private static List<ClassInfo> collectInterfaces(ClassInfo graphQLClientApi, IndexView index) {
        List<ClassInfo> interfaces = new ArrayList<>();
        Set<DotName> visitedInterfaces = new HashSet<>();
        Deque<ClassInfo> remainingInterfaces = new ArrayDeque<>();
        remainingInterfaces.add(graphQLClientApi);
        while (!remainingInterfaces.isEmpty()) {
            ClassInfo currentInterface = remainingInterfaces.poll();
            if (!visitedInterfaces.add(currentInterface.name())) {
                continue;
            }
            interfaces.add(currentInterface);
            for (DotName parentInterfaceName : currentInterface.interfaceNames()) {
                ClassInfo parentInterface = index.getClassByName(parentInterfaceName);
                if (parentInterface != null) {
                    remainingInterfaces.add(parentInterface);
                }
            }
        }
        return interfaces;
    }

    static class IsEnabled implements BooleanSupplier {
        OidcTokenPropagationGraphQLBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled();
        }
    }
}
