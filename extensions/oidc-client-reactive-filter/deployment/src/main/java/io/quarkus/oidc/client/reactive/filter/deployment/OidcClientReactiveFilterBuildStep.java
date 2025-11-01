package io.quarkus.oidc.client.reactive.filter.deployment;

import static io.quarkus.arc.processor.DotNames.SINGLETON;
import static io.quarkus.oidc.client.deployment.OidcClientFilterDeploymentHelper.DEFAULT_OIDC_REQUEST_FILTER_NAME;
import static io.quarkus.oidc.client.deployment.OidcClientFilterDeploymentHelper.detectCustomFiltersThatRequireResponseFilter;

import java.util.Collection;
import java.util.List;

import jakarta.ws.rs.Priorities;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.oidc.client.deployment.OidcClientBuildStep.IsEnabled;
import io.quarkus.oidc.client.deployment.OidcClientFilterDeploymentHelper;
import io.quarkus.oidc.client.filter.OidcClientFilter;
import io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter;
import io.quarkus.oidc.client.reactive.filter.runtime.AbstractOidcClientRequestReactiveFilter;
import io.quarkus.oidc.client.reactive.filter.runtime.DetectUnauthorizedClientResponseFilter;
import io.quarkus.oidc.client.reactive.filter.runtime.OidcClientReactiveFilterConfig;
import io.quarkus.rest.client.reactive.deployment.DotNames;
import io.quarkus.rest.client.reactive.deployment.RegisterProviderAnnotationInstanceBuildItem;

@BuildSteps(onlyIf = IsEnabled.class)
public class OidcClientReactiveFilterBuildStep {

    private static final DotName OIDC_CLIENT_FILTER = DotName.createSimple(OidcClientFilter.class.getName());
    OidcClientReactiveFilterConfig oidcClientReactiveFilterConfig;
    private static final DotName DETECT_401_RESPONSE_FILTER = DotName
            .createSimple(DetectUnauthorizedClientResponseFilter.class.getName());

    @BuildStep
    void oidcClientFilterSupport(CombinedIndexBuildItem indexBuildItem, BuildProducer<GeneratedBeanBuildItem> generatedBean,
            BuildProducer<RegisterProviderAnnotationInstanceBuildItem> producer) {
        final var helper = new OidcClientFilterDeploymentHelper<>(AbstractOidcClientRequestReactiveFilter.class, generatedBean,
                oidcClientReactiveFilterConfig.refreshOnUnauthorized());

        Collection<AnnotationInstance> instances = indexBuildItem.getIndex().getAnnotations(OIDC_CLIENT_FILTER);
        for (AnnotationInstance instance : instances) {

            // get client name from annotation @OidcClientFilter("clientName")
            String clientName = OidcClientFilterDeploymentHelper.getClientName(instance);
            if (clientName == null) {
                clientName = oidcClientReactiveFilterConfig.clientName().orElse(DEFAULT_OIDC_REQUEST_FILTER_NAME);
            }

            // we generate exactly one custom filter for each named client specified through annotation
            final AnnotationValue valueAttr = createClassValue(helper.getOrCreateFilter(clientName, instance));

            final AnnotationValue priorityAttr = AnnotationValue.createIntegerValue("priority", Priorities.AUTHENTICATION);
            String targetClass = OidcClientFilterDeploymentHelper.getTargetRestClientName(instance);
            producer.produce(new RegisterProviderAnnotationInstanceBuildItem(targetClass, AnnotationInstance.create(
                    DotNames.REGISTER_PROVIDER, OidcClientFilterDeploymentHelper.getTargetRestClient(instance),
                    List.of(valueAttr, priorityAttr))));

            if (oidcClientReactiveFilterConfig.refreshOnUnauthorized()) {
                var valueAttribute = createClassValue(DETECT_401_RESPONSE_FILTER);
                // client filter responsibility must be the other way around for response because
                // currently the MicroProfileRestClientResponseFilter which handles failures runs with priority USER
                var priority = AnnotationValue.createIntegerValue("priority", Priorities.USER + 100);
                producer.produce(new RegisterProviderAnnotationInstanceBuildItem(targetClass, AnnotationInstance
                        .create(DotNames.REGISTER_PROVIDER, OidcClientFilterDeploymentHelper.getTargetRestClient(instance),
                                List.of(valueAttribute, priority))));
            }
        }
    }

    private AnnotationValue createClassValue(DotName filter) {
        return AnnotationValue.createClassValue("value",
                Type.create(filter, Type.Kind.CLASS));
    }

    @BuildStep
    void registerProvider(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClassesBuildItem) {
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(OidcClientRequestReactiveFilter.class)
                .setUnremovable().setDefaultScope(SINGLETON).build());
        additionalIndexedClassesBuildItem
                .produce(new AdditionalIndexedClassesBuildItem(OidcClientRequestReactiveFilter.class.getName()));
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(OidcClientRequestReactiveFilter.class)
                .reason(getClass().getName())
                .methods().fields().build());
    }

    @BuildStep
    void registerDetectUnauthorizedResponseFilterForCustomFilters(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<RegisterProviderAnnotationInstanceBuildItem> producer, CombinedIndexBuildItem indexBuildItem) {
        var annotatedRestClientNames = detectCustomFiltersThatRequireResponseFilter(
                AbstractOidcClientRequestReactiveFilter.class, RegisterProvider.class, indexBuildItem.getIndex());
        boolean detectionEnabledForCustomFilters = !annotatedRestClientNames.isEmpty();
        if (detectionEnabledForCustomFilters) {
            // client filter responsibility must be the other way around for response because
            // currently the MicroProfileRestClientResponseFilter which handles failures runs with priority USER
            final var priority = AnnotationValue.createIntegerValue("priority", Priorities.USER + 100);
            final var annotationValues = List.of(createClassValue(DETECT_401_RESPONSE_FILTER), priority);
            annotatedRestClientNames.forEach(restClientName -> {
                String targetClass = restClientName.name().toString();
                producer.produce(new RegisterProviderAnnotationInstanceBuildItem(targetClass, AnnotationInstance
                        .create(DotNames.REGISTER_PROVIDER, restClientName, annotationValues)));
            });
        }
        if (oidcClientReactiveFilterConfig.refreshOnUnauthorized() || detectionEnabledForCustomFilters) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(DETECT_401_RESPONSE_FILTER.toString())
                    .constructors().methods().reason(getClass().getName()).serialization(true).build());
        }
    }
}
