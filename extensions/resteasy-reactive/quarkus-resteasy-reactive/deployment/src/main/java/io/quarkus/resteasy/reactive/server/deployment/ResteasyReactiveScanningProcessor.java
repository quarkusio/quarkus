package io.quarkus.resteasy.reactive.server.deployment;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Priorities;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.index.IndexingUtil;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.rest.server.runtime.exceptionmappers.AuthenticationCompletionExceptionMapper;
import io.quarkus.rest.server.runtime.exceptionmappers.AuthenticationFailedExceptionMapper;
import io.quarkus.rest.server.runtime.exceptionmappers.AuthenticationRedirectExceptionMapper;
import io.quarkus.rest.server.runtime.exceptionmappers.ForbiddenExceptionMapper;
import io.quarkus.rest.server.runtime.exceptionmappers.UnauthorizedExceptionMapper;
import io.quarkus.resteasy.reactive.common.deployment.ApplicationResultBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.QuarkusRestCommonProcessor;
import io.quarkus.resteasy.reactive.spi.ContainerRequestFilterBuildItem;
import io.quarkus.resteasy.reactive.spi.ContainerResponseFilterBuildItem;
import io.quarkus.resteasy.reactive.spi.ContextResolverBuildItem;
import io.quarkus.resteasy.reactive.spi.CustomContainerRequestFilterBuildItem;
import io.quarkus.resteasy.reactive.spi.CustomContainerResponseFilterBuildItem;
import io.quarkus.resteasy.reactive.spi.DynamicFeatureBuildItem;
import io.quarkus.resteasy.reactive.spi.ExceptionMapperBuildItem;
import io.quarkus.resteasy.reactive.spi.JaxrsFeatureBuildItem;
import io.quarkus.resteasy.reactive.spi.ParamConverterBuildItem;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.AuthenticationRedirectException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;

/**
 * Processor that handles scanning for types and turning them into build items
 */
public class ResteasyReactiveScanningProcessor {

    @BuildStep
    public void scanForFilters(CombinedIndexBuildItem combinedIndexBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            BuildProducer<ContainerRequestFilterBuildItem> requestFilterBuildItemBuildProducer,
            BuildProducer<ContainerResponseFilterBuildItem> responseFilterBuildItemBuildProducer) {
        IndexView index = combinedIndexBuildItem.getIndex();
        //the quarkus version of these filters will not be in the index
        //so you need an explicit check for both
        Collection<ClassInfo> containerResponseFilters = new HashSet<>(index
                .getAllKnownImplementors(ResteasyReactiveDotNames.CONTAINER_RESPONSE_FILTER));
        containerResponseFilters.addAll(index
                .getAllKnownImplementors(ResteasyReactiveDotNames.QUARKUS_REST_CONTAINER_RESPONSE_FILTER));
        Collection<ClassInfo> containerRequestFilters = new HashSet<>(index
                .getAllKnownImplementors(ResteasyReactiveDotNames.CONTAINER_REQUEST_FILTER));
        containerRequestFilters.addAll(index
                .getAllKnownImplementors(ResteasyReactiveDotNames.QUARKUS_REST_CONTAINER_REQUEST_FILTER));
        for (ClassInfo filterClass : containerRequestFilters) {
            QuarkusRestCommonProcessor.handleDiscoveredInterceptor(applicationResultBuildItem,
                    requestFilterBuildItemBuildProducer, index, filterClass,
                    ContainerRequestFilterBuildItem.Builder::new);
        }
        for (ClassInfo filterClass : containerResponseFilters) {
            QuarkusRestCommonProcessor.handleDiscoveredInterceptor(applicationResultBuildItem,
                    responseFilterBuildItemBuildProducer, index, filterClass,
                    ContainerResponseFilterBuildItem.Builder::new);
        }
    }

    @BuildStep
    public void scanForExceptionMappers(CombinedIndexBuildItem combinedIndexBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            BuildProducer<ExceptionMapperBuildItem> exceptionMapperBuildItemBuildProducer) {

        IndexView index = combinedIndexBuildItem.getComputingIndex();
        Collection<ClassInfo> exceptionMappers = index
                .getAllKnownImplementors(ResteasyReactiveDotNames.EXCEPTION_MAPPER);
        for (ClassInfo mapperClass : exceptionMappers) {
            ApplicationResultBuildItem.KeepProviderResult keepProviderResult = applicationResultBuildItem
                    .keepProvider(mapperClass);
            if (keepProviderResult != ApplicationResultBuildItem.KeepProviderResult.DISCARD) {
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(mapperClass.name(),
                        ResteasyReactiveDotNames.EXCEPTION_MAPPER,
                        index);
                DotName handledExceptionDotName = typeParameters.get(0).name();
                AnnotationInstance priorityInstance = mapperClass.classAnnotation(ResteasyReactiveDotNames.PRIORITY);
                int priority = Priorities.USER;
                if (priorityInstance != null) {
                    priority = priorityInstance.value().asInt();
                }
                exceptionMapperBuildItemBuildProducer.produce(new ExceptionMapperBuildItem(mapperClass.name().toString(),
                        handledExceptionDotName.toString(), priority, true));
            }
        }

        // built-ins
        exceptionMapperBuildItemBuildProducer.produce(new ExceptionMapperBuildItem(
                AuthenticationCompletionExceptionMapper.class.getName(),
                AuthenticationCompletionException.class.getName(),
                Priorities.USER, false));
        exceptionMapperBuildItemBuildProducer.produce(new ExceptionMapperBuildItem(
                AuthenticationFailedExceptionMapper.class.getName(),
                AuthenticationFailedException.class.getName(),
                Priorities.USER + 1, false));
        exceptionMapperBuildItemBuildProducer.produce(new ExceptionMapperBuildItem(
                AuthenticationRedirectExceptionMapper.class.getName(),
                AuthenticationRedirectException.class.getName(),
                Priorities.USER, false));
        exceptionMapperBuildItemBuildProducer.produce(new ExceptionMapperBuildItem(
                ForbiddenExceptionMapper.class.getName(),
                ForbiddenException.class.getName(),
                Priorities.USER + 1, false));
        exceptionMapperBuildItemBuildProducer.produce(new ExceptionMapperBuildItem(
                UnauthorizedExceptionMapper.class.getName(),
                UnauthorizedException.class.getName(),
                Priorities.USER + 1, false));
    }

    @BuildStep
    public void scanForDynamicFeatures(CombinedIndexBuildItem combinedIndexBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            BuildProducer<DynamicFeatureBuildItem> dynamicFeatureBuildItemBuildProducer) {
        IndexView index = combinedIndexBuildItem.getComputingIndex();
        Collection<ClassInfo> dynamicFeatures = index
                .getAllKnownImplementors(ResteasyReactiveDotNames.DYNAMIC_FEATURE);

        for (ClassInfo dynamicFeatureClass : dynamicFeatures) {
            ApplicationResultBuildItem.KeepProviderResult keepProviderResult = applicationResultBuildItem
                    .keepProvider(dynamicFeatureClass);
            if (keepProviderResult != ApplicationResultBuildItem.KeepProviderResult.DISCARD) {
                dynamicFeatureBuildItemBuildProducer
                        .produce(new DynamicFeatureBuildItem(dynamicFeatureClass.name().toString(), true));
            }
        }
    }

    @BuildStep
    public void scanForFeatures(CombinedIndexBuildItem combinedIndexBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            BuildProducer<JaxrsFeatureBuildItem> featureBuildItemBuildProducer) {
        IndexView index = combinedIndexBuildItem.getComputingIndex();
        Collection<ClassInfo> dynamicFeatures = index
                .getAllKnownImplementors(ResteasyReactiveDotNames.FEATURE);

        for (ClassInfo dynamicFeatureClass : dynamicFeatures) {
            ApplicationResultBuildItem.KeepProviderResult keepProviderResult = applicationResultBuildItem
                    .keepProvider(dynamicFeatureClass);
            if (keepProviderResult != ApplicationResultBuildItem.KeepProviderResult.DISCARD) {
                featureBuildItemBuildProducer
                        .produce(new JaxrsFeatureBuildItem(dynamicFeatureClass.name().toString(), true));
            }
        }
    }

    @BuildStep
    public void scanForContextResolvers(CombinedIndexBuildItem combinedIndexBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            BuildProducer<ContextResolverBuildItem> contextResolverBuildItemBuildProducer) {
        IndexView index = combinedIndexBuildItem.getComputingIndex();
        Collection<ClassInfo> contextResolvers = index
                .getAllKnownImplementors(ResteasyReactiveDotNames.CONTEXT_RESOLVER);

        for (ClassInfo resolverClass : contextResolvers) {
            ApplicationResultBuildItem.KeepProviderResult keepProviderResult = applicationResultBuildItem
                    .keepProvider(resolverClass);
            if (keepProviderResult != ApplicationResultBuildItem.KeepProviderResult.DISCARD) {
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(resolverClass.name(),
                        ResteasyReactiveDotNames.CONTEXT_RESOLVER,
                        index);
                contextResolverBuildItemBuildProducer.produce(new ContextResolverBuildItem(resolverClass.name().toString(),
                        typeParameters.get(0).name().toString(), getProducesMediaTypes(resolverClass), true));
            }
        }
    }

    @BuildStep
    public void scanForParamConverters(CombinedIndexBuildItem combinedIndexBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            BuildProducer<ParamConverterBuildItem> paramConverterBuildItemBuildProducer) {
        IndexView index = combinedIndexBuildItem.getComputingIndex();
        Collection<ClassInfo> paramConverterProviders = index
                .getAllKnownImplementors(ResteasyReactiveDotNames.PARAM_CONVERTER_PROVIDER);

        for (ClassInfo converterClass : paramConverterProviders) {
            ApplicationResultBuildItem.KeepProviderResult keepProviderResult = applicationResultBuildItem
                    .keepProvider(converterClass);
            if (keepProviderResult != ApplicationResultBuildItem.KeepProviderResult.DISCARD) {
                AnnotationInstance priorityInstance = converterClass.classAnnotation(ResteasyReactiveDotNames.PRIORITY);
                paramConverterBuildItemBuildProducer.produce(new ParamConverterBuildItem(converterClass.name().toString(),
                        priorityInstance != null ? priorityInstance.value().asInt() : Priorities.USER, true));
            }
        }

    }

    @BuildStep
    public void handleCustomProviders(
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<GeneratedBeanBuildItem> generatedBean,
            List<CustomContainerRequestFilterBuildItem> customContainerRequestFilters,
            List<CustomContainerResponseFilterBuildItem> customContainerResponseFilters,
            BuildProducer<ContainerRequestFilterBuildItem> additionalContainerRequestFilters,
            BuildProducer<ContainerResponseFilterBuildItem> additionalContainerResponseFilters,
            BuildProducer<AdditionalBeanBuildItem> additionalBean) {
        IndexView index = combinedIndexBuildItem.getComputingIndex();
        AdditionalBeanBuildItem.Builder additionalBeans = AdditionalBeanBuildItem.builder();

        // if we have custom filters, we need to index these classes
        if (!customContainerRequestFilters.isEmpty() || !customContainerResponseFilters.isEmpty()) {
            Indexer indexer = new Indexer();
            Set<DotName> additionalIndex = new HashSet<>();
            //we have to use the non-computing index here
            //the logic checks if the bean is already indexed, so the computing one breaks this
            for (CustomContainerRequestFilterBuildItem filter : customContainerRequestFilters) {
                IndexingUtil.indexClass(filter.getClassName(), indexer, combinedIndexBuildItem.getIndex(), additionalIndex,
                        Thread.currentThread().getContextClassLoader());
            }
            for (CustomContainerResponseFilterBuildItem filter : customContainerResponseFilters) {
                IndexingUtil.indexClass(filter.getClassName(), indexer, combinedIndexBuildItem.getIndex(), additionalIndex,
                        Thread.currentThread().getContextClassLoader());
            }
            index = CompositeIndex.create(index, indexer.complete());
        }

        for (AnnotationInstance instance : index
                .getAnnotations(ResteasyReactiveDotNames.CUSTOM_CONTAINER_REQUEST_FILTER)) {
            if (instance.target().kind() != AnnotationTarget.Kind.METHOD) {
                continue;
            }
            MethodInfo methodInfo = instance.target().asMethod();
            // the user class itself is made to be a bean as we want the user to be able to declare dependencies
            additionalBeans.addBeanClass(methodInfo.declaringClass().name().toString());
            String generatedClassName = CustomProviderGenerator.generateContainerRequestFilter(methodInfo,
                    new GeneratedBeanGizmoAdaptor(generatedBean));

            ContainerRequestFilterBuildItem.Builder builder = new ContainerRequestFilterBuildItem.Builder(generatedClassName)
                    .setRegisterAsBean(false); // it has already been made a bean
            AnnotationValue priorityValue = instance.value("priority");
            if (priorityValue != null) {
                builder.setPriority(priorityValue.asInt());
            }
            AnnotationValue preMatchingValue = instance.value("preMatching");
            if (preMatchingValue != null) {
                builder.setPreMatching(preMatchingValue.asBoolean());
            }
            additionalContainerRequestFilters.produce(builder.build());
        }
        for (AnnotationInstance instance : index
                .getAnnotations(ResteasyReactiveDotNames.CUSTOM_CONTAINER_RESPONSE_FILTER)) {
            if (instance.target().kind() != AnnotationTarget.Kind.METHOD) {
                continue;
            }
            MethodInfo methodInfo = instance.target().asMethod();
            // the user class itself is made to be a bean as we want the user to be able to declare dependencies
            additionalBeans.addBeanClass(methodInfo.declaringClass().name().toString());
            String generatedClassName = CustomProviderGenerator.generateContainerResponseFilter(methodInfo,
                    new GeneratedBeanGizmoAdaptor(generatedBean));
            ContainerResponseFilterBuildItem.Builder builder = new ContainerResponseFilterBuildItem.Builder(generatedClassName)
                    .setRegisterAsBean(false);// it has already been made a bean
            AnnotationValue priorityValue = instance.value("priority");
            if (priorityValue != null) {
                builder.setPriority(priorityValue.asInt());
            }
            additionalContainerResponseFilters.produce(builder.build());
        }

        additionalBean.produce(additionalBeans.setUnremovable().setDefaultScope(DotNames.SINGLETON).build());
    }

    private List<String> getProducesMediaTypes(ClassInfo classInfo) {
        AnnotationInstance produces = classInfo.classAnnotation(ResteasyReactiveDotNames.PRODUCES);
        if (produces == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(produces.value().asStringArray());
    }
}
