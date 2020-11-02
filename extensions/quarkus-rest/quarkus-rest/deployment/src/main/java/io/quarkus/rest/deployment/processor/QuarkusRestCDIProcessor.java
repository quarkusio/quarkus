package io.quarkus.rest.deployment.processor;

import java.util.List;

import javax.ws.rs.BeanParam;
import javax.ws.rs.core.Context;

import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AutoInjectAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.rest.common.deployment.framework.QuarkusRestDotNames;
import io.quarkus.rest.server.runtime.injection.ContextProducers;
import io.quarkus.rest.spi.ContainerRequestFilterBuildItem;
import io.quarkus.rest.spi.ContainerResponseFilterBuildItem;
import io.quarkus.rest.spi.ContextResolverBuildItem;
import io.quarkus.rest.spi.DynamicFeatureBuildItem;
import io.quarkus.rest.spi.ExceptionMapperBuildItem;
import io.quarkus.rest.spi.JaxrsFeatureBuildItem;

public class QuarkusRestCDIProcessor {

    @BuildStep
    AutoInjectAnnotationBuildItem contextInjection(
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer) {
        additionalBeanBuildItemBuildProducer
                .produce(AdditionalBeanBuildItem.builder().addBeanClasses(ContextProducers.class)
                        .build());
        return new AutoInjectAnnotationBuildItem(DotName.createSimple(Context.class.getName()),
                DotName.createSimple(BeanParam.class.getName()));

    }

    @BuildStep
    void beanDefiningAnnotations(BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(QuarkusRestDotNames.PATH, BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(QuarkusRestDotNames.APPLICATION_PATH,
                        BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(QuarkusRestDotNames.PROVIDER,
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
