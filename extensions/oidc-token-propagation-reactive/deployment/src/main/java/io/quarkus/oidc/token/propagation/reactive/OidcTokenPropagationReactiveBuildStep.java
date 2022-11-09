package io.quarkus.oidc.token.propagation.reactive;

import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.oidc.token.propagation.AccessToken;
import io.quarkus.rest.client.reactive.deployment.DotNames;
import io.quarkus.rest.client.reactive.deployment.RegisterProviderAnnotationInstanceBuildItem;

@BuildSteps(onlyIf = OidcTokenPropagationReactiveBuildStep.IsEnabled.class)
public class OidcTokenPropagationReactiveBuildStep {

    private static final DotName ACCESS_TOKEN = DotName.createSimple(AccessToken.class.getName());
    private static final DotName ACCESS_TOKEN_REQUEST_REACTIVE_FILTER = DotName
            .createSimple(AccessTokenRequestReactiveFilter.class.getName());

    @BuildStep
    void oidcClientFilterSupport(CombinedIndexBuildItem indexBuildItem,
            BuildProducer<RegisterProviderAnnotationInstanceBuildItem> producer) {
        Collection<AnnotationInstance> instances = indexBuildItem.getIndex().getAnnotations(ACCESS_TOKEN);
        for (AnnotationInstance instance : instances) {
            String targetClass = instance.target().asClass().name().toString();
            producer.produce(new RegisterProviderAnnotationInstanceBuildItem(targetClass, AnnotationInstance.create(
                    DotNames.REGISTER_PROVIDER, instance.target(), List.of(AnnotationValue.createClassValue("value",
                            Type.create(ACCESS_TOKEN_REQUEST_REACTIVE_FILTER, org.jboss.jandex.Type.Kind.CLASS))))));
        }
    }

    @BuildStep
    void registerProvider(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClassesBuildItem) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(AccessTokenRequestReactiveFilter.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, AccessTokenRequestReactiveFilter.class));
        additionalIndexedClassesBuildItem
                .produce(new AdditionalIndexedClassesBuildItem(AccessTokenRequestReactiveFilter.class.getName()));

    }

    public static class IsEnabled implements BooleanSupplier {
        OidcTokenPropagationReactiveBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled;
        }
    }
}
