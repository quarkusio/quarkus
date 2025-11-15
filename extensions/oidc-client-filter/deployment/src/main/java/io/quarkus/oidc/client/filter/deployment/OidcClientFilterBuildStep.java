package io.quarkus.oidc.client.filter.deployment;

import static io.quarkus.oidc.client.deployment.OidcClientFilterDeploymentHelper.DEFAULT_OIDC_REQUEST_FILTER_NAME;
import static io.quarkus.oidc.client.deployment.OidcClientFilterDeploymentHelper.detectCustomFiltersThatRequireResponseFilter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.oidc.client.deployment.OidcClientBuildStep.IsEnabled;
import io.quarkus.oidc.client.deployment.OidcClientFilterDeploymentHelper;
import io.quarkus.oidc.client.filter.OidcClientFilter;
import io.quarkus.oidc.client.filter.OidcClientRequestFilter;
import io.quarkus.oidc.client.filter.runtime.AbstractOidcClientRequestFilter;
import io.quarkus.oidc.client.filter.runtime.DetectUnauthorizedClientResponseFilter;
import io.quarkus.oidc.client.filter.runtime.OidcClientFilterConfig;
import io.quarkus.restclient.deployment.RestClientPredicateProviderBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;

@BuildSteps(onlyIf = IsEnabled.class)
public class OidcClientFilterBuildStep {

    private static final DotName OIDC_CLIENT_FILTER = DotName.createSimple(OidcClientFilter.class.getName());

    OidcClientFilterConfig config;

    @BuildStep
    void registerProvider(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            NamedOidcClientFilterBuildItem namedOidcClientFilterBuildItem,
            BuildProducer<ResteasyJaxrsProviderBuildItem> jaxrsProviders,
            BuildProducer<RestClientPredicateProviderBuildItem> restPredicateProvider) {

        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(OidcClientRequestFilter.class));
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(OidcClientRequestFilter.class)
                .reason(getClass().getName())
                .methods().fields().build());

        // register default request filter provider against the rest of the clients (client != namedFilterClientClasses)
        if (config.registerFilter()) {
            final Set<String> namedFilterClientClasses = namedOidcClientFilterBuildItem.namedFilterClientClasses;
            if (namedFilterClientClasses.isEmpty()) {
                // register default request filter as global rest client provider
                jaxrsProviders.produce(new ResteasyJaxrsProviderBuildItem(OidcClientRequestFilter.class.getName()));
                if (config.refreshOnUnauthorized()) {
                    jaxrsProviders.produce(
                            new ResteasyJaxrsProviderBuildItem(DetectUnauthorizedClientResponseFilter.class.getName()));
                }
            } else {
                var isNotNamedFilterClient = new Predicate<ClassInfo>() {
                    // test whether the provider should be added restClientClassInfo
                    @Override
                    public boolean test(ClassInfo restClientClassInfo) {
                        // do not register default request filter as provider against Rest client with named filter
                        return !namedFilterClientClasses.contains(restClientClassInfo.name().toString());
                    }
                };
                // register all clients without @OidcClientFilter
                restPredicateProvider
                        .produce(new RestClientPredicateProviderBuildItem(OidcClientRequestFilter.class.getName(),
                                isNotNamedFilterClient));
                if (config.refreshOnUnauthorized()) {
                    restPredicateProvider.produce(new RestClientPredicateProviderBuildItem(
                            DetectUnauthorizedClientResponseFilter.class.getName(), isNotNamedFilterClient));
                }
            }
        }
    }

    @BuildStep
    NamedOidcClientFilterBuildItem registerNamedProviders(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            CombinedIndexBuildItem indexBuildItem,
            BuildProducer<RestClientPredicateProviderBuildItem> restPredicateProvider,
            BuildProducer<GeneratedBeanBuildItem> generatedBean) {

        // create and register request filter for each @OidcClientFilter annotation instance
        final var helper = new OidcClientFilterDeploymentHelper<>(AbstractOidcClientRequestFilter.class, generatedBean,
                config.refreshOnUnauthorized());
        Collection<AnnotationInstance> instances = indexBuildItem.getIndex().getAnnotations(OIDC_CLIENT_FILTER);
        final Set<String> namedFilterClientClasses = new HashSet<>();
        for (AnnotationInstance instance : instances) {
            // get client name from annotation @OidcClientFilter
            String clientName = OidcClientFilterDeploymentHelper.getClientName(instance);
            if (clientName == null) {
                clientName = config.clientName().orElse(DEFAULT_OIDC_REQUEST_FILTER_NAME);
            }

            // we generate exactly one custom filter for each @OidcClientFilter client specified through annotation
            final var generatedProvider = helper.getOrCreateNamedTokensProducerFor(clientName, instance);
            final var targetRestClient = OidcClientFilterDeploymentHelper.getTargetRestClientName(instance);
            namedFilterClientClasses.add(targetRestClient);

            // register for reflection
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(generatedProvider).methods()
                    .reason(getClass().getName())
                    .fields().serialization(true).build());

            var isAnnotatedRestClient = new Predicate<ClassInfo>() {
                // test whether the provider should be added restClientClassInfo
                @Override
                public boolean test(ClassInfo restClientClassInfo) {
                    return targetRestClient.equals(restClientClassInfo.name().toString());
                }
            };

            // register named request filter provider against Rest client
            restPredicateProvider.produce(new RestClientPredicateProviderBuildItem(generatedProvider,
                    isAnnotatedRestClient));

            if (config.refreshOnUnauthorized()) {
                restPredicateProvider
                        .produce(new RestClientPredicateProviderBuildItem(
                                DetectUnauthorizedClientResponseFilter.class.getName(), isAnnotatedRestClient));
            }
        }
        return new NamedOidcClientFilterBuildItem(namedFilterClientClasses);
    }

    @BuildStep
    void registerDetectUnauthorizedResponseFilterForCustomFilters(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<RestClientPredicateProviderBuildItem> restPredicateProvider, CombinedIndexBuildItem indexBuildItem) {
        var annotatedRestClientNames = detectCustomFiltersThatRequireResponseFilter(AbstractOidcClientRequestFilter.class,
                RegisterProvider.class, indexBuildItem.getIndex()).stream().map(ClassInfo::name).toList();
        boolean detectionEnabledForCustomFilters = !annotatedRestClientNames.isEmpty();
        if (detectionEnabledForCustomFilters) {
            // test whether the provider should be added restClientClassInfo
            restPredicateProvider
                    .produce(new RestClientPredicateProviderBuildItem(DetectUnauthorizedClientResponseFilter.class.getName(),
                            restClientClassInfo -> annotatedRestClientNames.contains(restClientClassInfo.name())));
        }
        if (config.refreshOnUnauthorized() || detectionEnabledForCustomFilters) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(DetectUnauthorizedClientResponseFilter.class.getName())
                    .constructors().methods().reason(getClass().getName()).serialization(true).build());
        }
    }
}
