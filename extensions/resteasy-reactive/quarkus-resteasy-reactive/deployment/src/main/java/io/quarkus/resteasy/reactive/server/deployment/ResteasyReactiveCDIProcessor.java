package io.quarkus.resteasy.reactive.server.deployment;

import java.util.List;

import javax.ws.rs.BeanParam;
import javax.ws.rs.core.Context;

import org.jboss.jandex.DotName;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.server.injection.ContextProducers;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AutoInjectAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.rest.server.runtime.QuarkusContextProducers;
import io.quarkus.resteasy.reactive.spi.ContainerRequestFilterBuildItem;
import io.quarkus.resteasy.reactive.spi.ContainerResponseFilterBuildItem;
import io.quarkus.resteasy.reactive.spi.ContextResolverBuildItem;
import io.quarkus.resteasy.reactive.spi.DynamicFeatureBuildItem;
import io.quarkus.resteasy.reactive.spi.ExceptionMapperBuildItem;
import io.quarkus.resteasy.reactive.spi.JaxrsFeatureBuildItem;

public class ResteasyReactiveCDIProcessor {

    @BuildStep
    AutoInjectAnnotationBuildItem contextInjection(
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer) {
        additionalBeanBuildItemBuildProducer
                .produce(AdditionalBeanBuildItem.builder().addBeanClasses(ContextProducers.class, QuarkusContextProducers.class)
                        .build());
        return new AutoInjectAnnotationBuildItem(DotName.createSimple(Context.class.getName()),
                DotName.createSimple(BeanParam.class.getName()));

    }

    @BuildStep
    void beanDefiningAnnotations(BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(ResteasyReactiveDotNames.PATH, BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(ResteasyReactiveDotNames.APPLICATION_PATH,
                        BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(ResteasyReactiveDotNames.PROVIDER,
                        BuiltinScope.SINGLETON.getName()));
    }

    @BuildStep
    void additionalBeans(List<ContainerRequestFilterBuildItem> additionalContainerRequestFilters,
            List<ContainerResponseFilterBuildItem> additionalContainerResponseFilters,
            List<DynamicFeatureBuildItem> additionalDynamicFeatures,
            List<ExceptionMapperBuildItem> additionalExceptionMappers,
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            List<JaxrsFeatureBuildItem> featureBuildItems,
            List<ContextResolverBuildItem> contextResolverBuildItems) {

        AdditionalBeanBuildItem.Builder additionalProviders = AdditionalBeanBuildItem.builder();
        for (ContainerRequestFilterBuildItem requestFilter : additionalContainerRequestFilters) {
            if (requestFilter.isRegisterAsBean()) {
                additionalProviders.addBeanClass(requestFilter.getClassName());
            }
        }
        for (ContainerResponseFilterBuildItem responseFilter : additionalContainerResponseFilters) {
            if (responseFilter.isRegisterAsBean()) {
                additionalProviders.addBeanClass(responseFilter.getClassName());
            }
        }
        for (ExceptionMapperBuildItem exceptionMapper : additionalExceptionMappers) {
            if (exceptionMapper.isRegisterAsBean()) {
                additionalProviders.addBeanClass(exceptionMapper.getClassName());
            }
        }
        for (DynamicFeatureBuildItem dynamicFeature : additionalDynamicFeatures) {
            if (dynamicFeature.isRegisterAsBean()) {
                additionalProviders.addBeanClass(dynamicFeature.getClassName());
            }
        }
        for (JaxrsFeatureBuildItem dynamicFeature : featureBuildItems) {
            if (dynamicFeature.isRegisterAsBean()) {
                additionalProviders.addBeanClass(dynamicFeature.getClassName());
            }
        }
        for (ContextResolverBuildItem contextResolver : contextResolverBuildItems) {
            if (contextResolver.isRegisterAsBean()) {
                additionalProviders.addBeanClass(contextResolver.getClassName());
            }
        }
        additionalBean.produce(additionalProviders.setUnremovable().setDefaultScope(DotNames.SINGLETON).build());
    }

}
