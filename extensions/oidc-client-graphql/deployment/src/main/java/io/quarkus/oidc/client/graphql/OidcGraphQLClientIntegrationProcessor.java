package io.quarkus.oidc.client.graphql;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.oidc.client.runtime.OidcClientsConfig.DEFAULT_CLIENT_KEY;

import java.lang.constant.ClassDesc;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmo2Adaptor;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.oidc.client.graphql.runtime.AbstractGraphQLTokenProvider;
import io.quarkus.oidc.client.graphql.runtime.OidcClientGraphQLConfig;
import io.quarkus.oidc.client.graphql.runtime.OidcGraphQLClientIntegrationRecorder;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.smallrye.graphql.client.deployment.GraphQLClientConfigInitializedBuildItem;

class OidcGraphQLClientIntegrationProcessor {

    private static final DotName GRAPHQL_CLIENT_API = DotName
            .createSimple("io.smallrye.graphql.client.typesafe.api.GraphQLClientApi");

    private static final String OIDC_CLIENT_FILTER = "io.quarkus.oidc.client.filter.OidcClientFilter";

    private static final String TOKEN_PROVIDER_CLASS = AbstractGraphQLTokenProvider.class.getName();

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> featureProducer) {
        featureProducer.produce(new FeatureBuildItem(Feature.OIDC_CLIENT_GRAPHQL_CLIENT_INTEGRATION));
    }

    @BuildStep
    GraphQLTokenProducerInfo collectGraphQLTokenProducerInfo(OidcClientGraphQLConfig config,
            CombinedIndexBuildItem combinedIndex) {
        var configKeysToOidcClientsFromIndex = getConfigKeysToOidcClientsFromIndex(combinedIndex.getIndex(), false);

        // named OIDC clients
        var configKeysToOidcClients = new HashMap<>(configKeysToOidcClientsFromIndex);
        config.additionalOidcClients().forEach((graphQlClient, oidcClient) -> {
            var previousOidcClient = configKeysToOidcClients.put(graphQlClient, oidcClient.clientName());
            if (previousOidcClient != null && !previousOidcClient.equals(oidcClient.clientName())) {
                throw new ConfigurationException("""
                        The '%s' annotation has been used to configure the '%s' OIDC client for the '%s' GraphQL client.
                        However, the 'quarkus.oidc-client-graphql."%s".client-name' configuration property
                        sets the OIDC client to '%s'. Please choose one way to configure the OIDC client name.
                        """.formatted(OIDC_CLIENT_FILTER, previousOidcClient, graphQlClient, graphQlClient,
                        oidcClient.clientName()));
            }
        });

        Map<String, String> oidcClientToTokenProducerBeanName = new HashMap<>();
        configKeysToOidcClients.values().forEach(oidcClient -> oidcClientToTokenProducerBeanName
                .putIfAbsent(oidcClient, generatedBeanClassName(oidcClient)));

        // default OIDC client used when the GraphQL client doesn't have explicitly assigned another client
        config.clientName().ifPresentOrElse(defaultOidcClientName -> oidcClientToTokenProducerBeanName
                .putIfAbsent(defaultOidcClientName, generatedBeanClassName(defaultOidcClientName)),
                () -> oidcClientToTokenProducerBeanName.put(DEFAULT_CLIENT_KEY, generatedBeanClassName(DEFAULT_CLIENT_KEY)));

        return new GraphQLTokenProducerInfo(oidcClientToTokenProducerBeanName, configKeysToOidcClientsFromIndex);
    }

    /**
     * For each OIDC client required by GraphQL clients, we generate a token producer like this:
     *
     * <pre>
     * {
     *     &#64;code
     *     &#64;Singleton
     *     @Unremovable
     *     public class AbstractGraphQLTokenProvider_OidcClient1 extends AbstractGraphQLTokenProvider {
     *         public AbstractGraphQLTokenProvider_OidcClient1() {
     *             super("OidcClient1");
     *         }
     *     }
     * }
     * </pre>
     */
    @BuildStep
    void generateGraphQLTokenProducers(GraphQLTokenProducerInfo graphQLTokenProducerInfo,
            BuildProducer<GeneratedBeanBuildItem> generatedBeanProducer) {
        graphQLTokenProducerInfo.oidcClientToTokenProducerBeanName.forEach((oidcClientName, generatedClassName) -> {
            ClassOutput classOutput = new GeneratedBeanGizmo2Adaptor(generatedBeanProducer);
            Gizmo gizmo = Gizmo.create(classOutput).withDebugInfo(false).withParameters(false);
            gizmo.class_(generatedClassName, cc -> {
                cc.public_();
                cc.addAnnotation(Singleton.class);
                cc.addAnnotation(Unremovable.class);
                var tokenProviderClassDesc = ClassDesc.of(TOKEN_PROVIDER_CLASS);
                cc.extends_(tokenProviderClassDesc);
                cc.constructor(ctorCreator -> {
                    ctorCreator.public_();
                    ctorCreator
                            .body(bc -> {
                                bc.invokeSpecial(ConstructorDesc.of(tokenProviderClassDesc, String.class), cc.this_(),
                                        Const.of(oidcClientName));
                                bc.return_();
                            });
                });
            });
        });
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void initialize(BeanContainerBuildItem containerBuildItem,
            OidcGraphQLClientIntegrationRecorder recorder,
            OidcClientGraphQLConfig config,
            BeanArchiveIndexBuildItem index,
            GraphQLTokenProducerInfo graphQLTokenProducerInfo,
            GraphQLClientConfigInitializedBuildItem configInitialized) {
        Map<String, String> configKeysToOidcClients = getConfigKeysToOidcClientsFromIndex(index.getIndex(), true);
        configKeysToOidcClients.forEach((graphQLConfigKey, oidcClient) -> {
            if (!graphQLTokenProducerInfo.configKeysToOidcClientsFromCombinedIndex.containsValue(oidcClient)) {
                // ATM GraphQL extension processor relies on the combined index as well, this validation is in place
                // so that we don't miss future changes
                throw new IllegalStateException("GraphQL client with config key '%s' wasn't present in the combined index,"
                        + " therefore Quarkus didn't generate required OIDC token provider");
            }
        });
        config.additionalOidcClients()
                .forEach((graphQlClient, oidcClient) -> configKeysToOidcClients.put(graphQlClient, oidcClient.clientName()));

        recorder.enhanceGraphQLClientConfigurationWithOidc(configKeysToOidcClients, config.clientName().orElse(null),
                graphQLTokenProducerInfo.oidcClientToTokenProducerBeanName);
    }

    private static Map<String, String> getConfigKeysToOidcClientsFromIndex(IndexView index, boolean withDefault) {
        Map<String, String> configKeysToOidcClients = new HashMap<>();
        for (AnnotationInstance annotation : index.getAnnotations(GRAPHQL_CLIENT_API)) {
            ClassInfo clazz = annotation.target().asClass();
            AnnotationInstance oidcClient = clazz.annotation(OIDC_CLIENT_FILTER);
            if (oidcClient != null) {

                final String oidcClientName;
                if (withDefault) {
                    oidcClientName = oidcClient.valueWithDefault(index, "value").asString();
                } else {
                    // combined index doesn't contain this annotation, and we don't really need it as if default really
                    // changed, the build time validation will start failing
                    var annotationValue = oidcClient.value("value");
                    oidcClientName = annotationValue == null ? null : annotationValue.asString();
                }

                AnnotationValue configKeyValue = annotation.value("configKey");
                String configKey = configKeyValue != null ? configKeyValue.asString() : null;
                String actualConfigKey = (configKey != null && !configKey.equals("")) ? configKey : clazz.name().toString();
                if (oidcClientName != null && !oidcClientName.isEmpty()) {
                    configKeysToOidcClients.put(actualConfigKey, oidcClientName);
                }
            }
        }
        return configKeysToOidcClients;
    }

    private static String generatedBeanClassName(String oidcClient) {
        return TOKEN_PROVIDER_CLASS + "_" + sanitizeOidcClientName(oidcClient);
    }

    private static String sanitizeOidcClientName(String oidcClientName) {
        return oidcClientName.replaceAll("\\W+", "");
    }

    private static final class GraphQLTokenProducerInfo extends SimpleBuildItem {

        private final Map<String, String> oidcClientToTokenProducerBeanName;
        private final Map<String, String> configKeysToOidcClientsFromCombinedIndex;

        private GraphQLTokenProducerInfo(Map<String, String> oidcClientToTokenProducerBeanName,
                Map<String, String> configKeysToOidcClientsFromCombinedIndex) {
            this.oidcClientToTokenProducerBeanName = Map.copyOf(oidcClientToTokenProducerBeanName);
            this.configKeysToOidcClientsFromCombinedIndex = Map.copyOf(configKeysToOidcClientsFromCombinedIndex);
        }
    }
}
