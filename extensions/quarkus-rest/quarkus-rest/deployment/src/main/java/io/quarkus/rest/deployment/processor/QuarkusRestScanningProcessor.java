package io.quarkus.rest.deployment.processor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.ws.rs.Priorities;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.rest.common.deployment.ApplicationResultBuildItem;
import io.quarkus.rest.common.deployment.QuarkusRestCommonProcessor;
import io.quarkus.rest.common.deployment.framework.QuarkusRestDotNames;
import io.quarkus.rest.server.runtime.providers.exceptionmappers.AuthenticationCompletionExceptionMapper;
import io.quarkus.rest.server.runtime.providers.exceptionmappers.AuthenticationFailedExceptionMapper;
import io.quarkus.rest.server.runtime.providers.exceptionmappers.AuthenticationRedirectExceptionMapper;
import io.quarkus.rest.server.runtime.providers.exceptionmappers.ForbiddenExceptionMapper;
import io.quarkus.rest.server.runtime.providers.exceptionmappers.UnauthorizedExceptionMapper;
import io.quarkus.rest.spi.ContainerRequestFilterBuildItem;
import io.quarkus.rest.spi.ContainerResponseFilterBuildItem;
import io.quarkus.rest.spi.ContextResolverBuildItem;
import io.quarkus.rest.spi.DynamicFeatureBuildItem;
import io.quarkus.rest.spi.ExceptionMapperBuildItem;
import io.quarkus.rest.spi.JaxrsFeatureBuildItem;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.AuthenticationRedirectException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;

/**
 * Processor that handles scanning for types and turning them into build items
 */
public class QuarkusRestScanningProcessor {

    @BuildStep
    public void scanForFilters(CombinedIndexBuildItem combinedIndexBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            BuildProducer<ContainerRequestFilterBuildItem> requestFilterBuildItemBuildProducer,
            BuildProducer<ContainerResponseFilterBuildItem> responseFilterBuildItemBuildProducer) {
        IndexView index = combinedIndexBuildItem.getIndex();
        //the quarkus version of these filters will not be in the index
        //so you need an explicit check for both
        Collection<ClassInfo> containerResponseFilters = new HashSet<>(index
                .getAllKnownImplementors(QuarkusRestDotNames.CONTAINER_RESPONSE_FILTER));
        containerResponseFilters.addAll(index
                .getAllKnownImplementors(QuarkusRestDotNames.QUARKUS_REST_CONTAINER_RESPONSE_FILTER));
        Collection<ClassInfo> containerRequestFilters = new HashSet<>(index
                .getAllKnownImplementors(QuarkusRestDotNames.CONTAINER_REQUEST_FILTER));
        containerRequestFilters.addAll(index
                .getAllKnownImplementors(QuarkusRestDotNames.QUARKUS_REST_CONTAINER_REQUEST_FILTER));
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
                .getAllKnownImplementors(QuarkusRestDotNames.EXCEPTION_MAPPER);
        for (ClassInfo mapperClass : exceptionMappers) {
            ApplicationResultBuildItem.KeepProviderResult keepProviderResult = applicationResultBuildItem
                    .keepProvider(mapperClass);
            if (keepProviderResult != ApplicationResultBuildItem.KeepProviderResult.DISCARD) {
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(mapperClass.name(),
                        QuarkusRestDotNames.EXCEPTION_MAPPER,
                        index);
                DotName handledExceptionDotName = typeParameters.get(0).name();
                AnnotationInstance priorityInstance = mapperClass.classAnnotation(QuarkusRestDotNames.PRIORITY);
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
                .getAllKnownImplementors(QuarkusRestDotNames.DYNAMIC_FEATURE);

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
                .getAllKnownImplementors(QuarkusRestDotNames.FEATURE);

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
                .getAllKnownImplementors(QuarkusRestDotNames.CONTEXT_RESOLVER);

        for (ClassInfo resolverClass : contextResolvers) {
            ApplicationResultBuildItem.KeepProviderResult keepProviderResult = applicationResultBuildItem
                    .keepProvider(resolverClass);
            if (keepProviderResult != ApplicationResultBuildItem.KeepProviderResult.DISCARD) {
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(resolverClass.name(),
                        QuarkusRestDotNames.CONTEXT_RESOLVER,
                        index);
                contextResolverBuildItemBuildProducer.produce(new ContextResolverBuildItem(resolverClass.name().toString(),
                        typeParameters.get(0).name().toString(), getProducesMediaTypes(resolverClass), true));
            }
        }
    }

    private List<String> getProducesMediaTypes(ClassInfo classInfo) {
        AnnotationInstance produces = classInfo.classAnnotation(QuarkusRestDotNames.PRODUCES);
        if (produces == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(produces.value().asStringArray());
    }
}
