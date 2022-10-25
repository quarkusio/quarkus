package io.quarkus.resteasy.reactive.server.deployment;

import static io.quarkus.resteasy.reactive.common.deployment.QuarkusResteasyReactiveDotNames.HTTP_SERVER_REQUEST;
import static io.quarkus.resteasy.reactive.common.deployment.QuarkusResteasyReactiveDotNames.HTTP_SERVER_RESPONSE;
import static io.quarkus.resteasy.reactive.common.deployment.QuarkusResteasyReactiveDotNames.ROUTING_CONTEXT;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.transaction.RollbackException;
import jakarta.ws.rs.Priorities;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.common.core.BlockingNotAllowedException;
import org.jboss.resteasy.reactive.common.model.ResourceContextResolver;
import org.jboss.resteasy.reactive.common.model.ResourceExceptionMapper;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptors;
import org.jboss.resteasy.reactive.common.model.ResourceParamConverterProvider;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.scanning.ApplicationScanningResult;
import org.jboss.resteasy.reactive.common.processor.scanning.ResteasyReactiveInterceptorScanner;
import org.jboss.resteasy.reactive.server.core.ExceptionMapping;
import org.jboss.resteasy.reactive.server.model.ContextResolvers;
import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;
import org.jboss.resteasy.reactive.server.processor.generation.exceptionmappers.ServerExceptionMapperGenerator;
import org.jboss.resteasy.reactive.server.processor.generation.filters.FilterGeneration;
import org.jboss.resteasy.reactive.server.processor.scanning.AsyncReturnTypeScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.CacheControlScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.ResteasyReactiveContextResolverScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.ResteasyReactiveExceptionMappingScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.ResteasyReactiveFeatureScanner;

import io.quarkus.arc.ArcUndeclaredThrowableException;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.index.IndexingUtil;
import io.quarkus.resteasy.reactive.common.deployment.ApplicationResultBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.ResourceInterceptorsContributorBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.ResourceScanningResultBuildItem;
import io.quarkus.resteasy.reactive.server.spi.MethodScannerBuildItem;
import io.quarkus.resteasy.reactive.server.spi.UnwrappedExceptionBuildItem;
import io.quarkus.resteasy.reactive.spi.ContainerRequestFilterBuildItem;
import io.quarkus.resteasy.reactive.spi.ContainerResponseFilterBuildItem;
import io.quarkus.resteasy.reactive.spi.ContextResolverBuildItem;
import io.quarkus.resteasy.reactive.spi.CustomContainerRequestFilterBuildItem;
import io.quarkus.resteasy.reactive.spi.CustomContainerResponseFilterBuildItem;
import io.quarkus.resteasy.reactive.spi.CustomExceptionMapperBuildItem;
import io.quarkus.resteasy.reactive.spi.DynamicFeatureBuildItem;
import io.quarkus.resteasy.reactive.spi.ExceptionMapperBuildItem;
import io.quarkus.resteasy.reactive.spi.JaxrsFeatureBuildItem;
import io.quarkus.resteasy.reactive.spi.ParamConverterBuildItem;
import io.quarkus.runtime.BlockingOperationNotAllowedException;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;

/**
 * Processor that handles scanning for types and turning them into build items
 */
public class ResteasyReactiveScanningProcessor {

    @BuildStep
    public MethodScannerBuildItem asyncSupport() {
        return new MethodScannerBuildItem(new AsyncReturnTypeScanner());
    }

    @BuildStep
    public MethodScannerBuildItem cacheControlSupport() {
        return new MethodScannerBuildItem(new CacheControlScanner());
    }

    @BuildStep
    public MethodScannerBuildItem compressionSupport(HttpBuildTimeConfig httpBuildTimeConfig) {
        return new MethodScannerBuildItem(new CompressionScanner(httpBuildTimeConfig));
    }

    @BuildStep
    public ResourceInterceptorsContributorBuildItem scanForInterceptors(CombinedIndexBuildItem combinedIndexBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem) {
        return new ResourceInterceptorsContributorBuildItem(new Consumer<ResourceInterceptors>() {
            @Override
            public void accept(ResourceInterceptors interceptors) {
                ResteasyReactiveInterceptorScanner.scanForInterceptors(interceptors, combinedIndexBuildItem.getIndex(),
                        applicationResultBuildItem.getResult());
            }
        });
    }

    @BuildStep
    public List<UnwrappedExceptionBuildItem> defaultUnwrappedException() {
        return List.of(new UnwrappedExceptionBuildItem(ArcUndeclaredThrowableException.class),
                new UnwrappedExceptionBuildItem(RollbackException.class));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BuildStep
    public ExceptionMappersBuildItem scanForExceptionMappers(CombinedIndexBuildItem combinedIndexBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer,
            List<ExceptionMapperBuildItem> mappers, List<UnwrappedExceptionBuildItem> unwrappedExceptions,
            Capabilities capabilities) {
        AdditionalBeanBuildItem.Builder beanBuilder = AdditionalBeanBuildItem.builder().setUnremovable();
        ExceptionMapping exceptions = ResteasyReactiveExceptionMappingScanner
                .scanForExceptionMappers(combinedIndexBuildItem.getComputingIndex(), applicationResultBuildItem.getResult());

        exceptions.addBlockingProblem(BlockingOperationNotAllowedException.class);
        exceptions.addBlockingProblem(BlockingNotAllowedException.class);
        for (UnwrappedExceptionBuildItem bi : unwrappedExceptions) {
            exceptions.addUnwrappedException(bi.getThrowableClass().getName());
        }
        if (capabilities.isPresent(Capability.HIBERNATE_REACTIVE)) {
            exceptions.addNonBlockingProblem(
                    new ExceptionMapping.ExceptionTypeAndMessageContainsPredicate(IllegalStateException.class, "HR000068"));
        }

        for (Map.Entry<String, ResourceExceptionMapper<? extends Throwable>> i : exceptions.getMappers()
                .entrySet()) {
            beanBuilder.addBeanClass(i.getValue().getClassName());
        }
        for (ExceptionMapperBuildItem additionalExceptionMapper : mappers) {
            if (additionalExceptionMapper.isRegisterAsBean()) {
                beanBuilder.addBeanClass(additionalExceptionMapper.getClassName());
            } else {
                reflectiveClassBuildItemBuildProducer
                        .produce(new ReflectiveClassBuildItem(true, false, false, additionalExceptionMapper.getClassName()));
            }
            int priority = Priorities.USER;
            if (additionalExceptionMapper.getPriority() != null) {
                priority = additionalExceptionMapper.getPriority();
            }
            ResourceExceptionMapper<Throwable> mapper = new ResourceExceptionMapper<>();
            mapper.setPriority(priority);
            mapper.setClassName(additionalExceptionMapper.getClassName());
            exceptions.addExceptionMapper(additionalExceptionMapper.getHandledExceptionName(), mapper);
        }
        additionalBeanBuildItemBuildProducer.produce(beanBuilder.build());
        return new ExceptionMappersBuildItem(exceptions);
    }

    @BuildStep
    public ParamConverterProvidersBuildItem scanForParamConverters(
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer,
            List<ParamConverterBuildItem> paramConverterBuildItems) {

        AdditionalBeanBuildItem.Builder beanBuilder = AdditionalBeanBuildItem.builder().setUnremovable();
        ParamConverterProviders paramConverterProviders = new ParamConverterProviders();
        for (ParamConverterBuildItem additionalParamConverter : paramConverterBuildItems) {
            if (additionalParamConverter.isRegisterAsBean()) {
                beanBuilder.addBeanClass(additionalParamConverter.getClassName());
            } else {
                reflectiveClassBuildItemBuildProducer
                        .produce(new ReflectiveClassBuildItem(true, false, false, additionalParamConverter.getClassName()));
            }
            int priority = Priorities.USER;
            if (additionalParamConverter.getPriority() != null) {
                priority = additionalParamConverter.getPriority();
            }
            ResourceParamConverterProvider provider = new ResourceParamConverterProvider();
            provider.setPriority(priority);
            provider.setClassName(additionalParamConverter.getClassName());
            paramConverterProviders.addParamConverterProviders(provider);
        }
        additionalBeanBuildItemBuildProducer.produce(beanBuilder.build());
        return new ParamConverterProvidersBuildItem(paramConverterProviders);

    }

    @BuildStep
    public void scanForDynamicFeatures(CombinedIndexBuildItem combinedIndexBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            BuildProducer<DynamicFeatureBuildItem> dynamicFeatureBuildItemBuildProducer) {
        IndexView index = combinedIndexBuildItem.getComputingIndex();
        Set<String> features = ResteasyReactiveFeatureScanner.scanForDynamicFeatures(index,
                applicationResultBuildItem.getResult());
        for (String dynamicFeatureClass : features) {
            dynamicFeatureBuildItemBuildProducer
                    .produce(new DynamicFeatureBuildItem(dynamicFeatureClass, true));
        }
    }

    @BuildStep
    public void scanForFeatures(CombinedIndexBuildItem combinedIndexBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            BuildProducer<JaxrsFeatureBuildItem> featureBuildItemBuildProducer) {
        IndexView index = combinedIndexBuildItem.getComputingIndex();
        Set<String> features = ResteasyReactiveFeatureScanner.scanForFeatures(index, applicationResultBuildItem.getResult());
        for (String feature : features) {
            featureBuildItemBuildProducer
                    .produce(new JaxrsFeatureBuildItem(feature, true));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BuildStep
    public ContextResolversBuildItem scanForContextResolvers(CombinedIndexBuildItem combinedIndexBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer,
            List<ContextResolverBuildItem> additionalResolvers) {
        IndexView index = combinedIndexBuildItem.getComputingIndex();
        AdditionalBeanBuildItem.Builder beanBuilder = AdditionalBeanBuildItem.builder().setUnremovable();
        ContextResolvers resolvers = ResteasyReactiveContextResolverScanner.scanForContextResolvers(index,
                applicationResultBuildItem.getResult());
        for (Map.Entry<Class<?>, List<ResourceContextResolver>> entry : resolvers.getResolvers().entrySet()) {
            for (ResourceContextResolver i : entry.getValue()) {
                beanBuilder.addBeanClass(i.getClassName());
            }
        }
        for (ContextResolverBuildItem i : additionalResolvers) {
            if (i.isRegisterAsBean()) {
                beanBuilder.addBeanClass(i.getClassName());
            } else {
                reflectiveClassBuildItemBuildProducer
                        .produce(new ReflectiveClassBuildItem(true, false, false, i.getClassName()));
            }
            ResourceContextResolver resolver = new ResourceContextResolver();
            resolver.setClassName(i.getClassName());
            resolver.setMediaTypeStrings(i.getMediaTypes());
            try {
                resolvers.addContextResolver((Class) Class.forName(i.getProvidedType(), false,
                        Thread.currentThread().getContextClassLoader()), resolver);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(
                        "Unable to load handled exception type " + i.getProvidedType(), e);
            }
        }
        additionalBeanBuildItemBuildProducer.produce(beanBuilder.build());
        return new ContextResolversBuildItem(resolvers);
    }

    @BuildStep
    public void scanForParamConverters(CombinedIndexBuildItem combinedIndexBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            BuildProducer<ParamConverterBuildItem> paramConverterBuildItemBuildProducer) {
        IndexView index = combinedIndexBuildItem.getComputingIndex();
        Collection<ClassInfo> paramConverterProviders = index
                .getAllKnownImplementors(ResteasyReactiveDotNames.PARAM_CONVERTER_PROVIDER);

        for (ClassInfo converterClass : paramConverterProviders) {
            ApplicationScanningResult.KeepProviderResult keepProviderResult = applicationResultBuildItem.getResult()
                    .keepProvider(converterClass);
            if (keepProviderResult != ApplicationScanningResult.KeepProviderResult.DISCARD) {
                AnnotationInstance priorityInstance = converterClass.classAnnotation(ResteasyReactiveDotNames.PRIORITY);
                paramConverterBuildItemBuildProducer.produce(new ParamConverterBuildItem(converterClass.name().toString(),
                        priorityInstance != null ? priorityInstance.value().asInt() : Priorities.USER, true));
            }
        }

    }

    @BuildStep
    public void handleCustomAnnotatedMethods(
            Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<GeneratedBeanBuildItem> generatedBean,
            List<CustomContainerRequestFilterBuildItem> customContainerRequestFilters,
            List<CustomContainerResponseFilterBuildItem> customContainerResponseFilters,
            List<CustomExceptionMapperBuildItem> customExceptionMappers,
            BuildProducer<ContainerRequestFilterBuildItem> additionalContainerRequestFilters,
            BuildProducer<ContainerResponseFilterBuildItem> additionalContainerResponseFilters,
            BuildProducer<ExceptionMapperBuildItem> additionalExceptionMappers,
            BuildProducer<AdditionalBeanBuildItem> additionalBean) {
        IndexView index = combinedIndexBuildItem.getComputingIndex();
        AdditionalBeanBuildItem.Builder additionalBeans = AdditionalBeanBuildItem.builder();

        // if we have custom filters, we need to index these classes
        if (!customContainerRequestFilters.isEmpty() || !customContainerResponseFilters.isEmpty()
                || !customExceptionMappers.isEmpty()) {
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
            for (CustomExceptionMapperBuildItem mapper : customExceptionMappers) {
                IndexingUtil.indexClass(mapper.getClassName(), indexer, combinedIndexBuildItem.getIndex(), additionalIndex,
                        Thread.currentThread().getContextClassLoader());
            }
            index = CompositeIndex.create(index, indexer.complete());
        }

        List<FilterGeneration.GeneratedFilter> generatedFilters = FilterGeneration.generate(index,
                Set.of(HTTP_SERVER_REQUEST, HTTP_SERVER_RESPONSE, ROUTING_CONTEXT), Set.of(Unremovable.class.getName()));
        for (var generated : generatedFilters) {
            for (var i : generated.getGeneratedClasses()) {
                generatedBean.produce(new GeneratedBeanBuildItem(i.getName(), i.getData()));
            }
            if (generated.isRequestFilter()) {
                // the user class itself is made to be a bean as we want the user to be able to declare dependencies
                additionalBeans.addBeanClass(generated.getDeclaringClassName());
                ContainerRequestFilterBuildItem.Builder builder = new ContainerRequestFilterBuildItem.Builder(
                        generated.getGeneratedClassName())
                        .setRegisterAsBean(false) // it has already been made a bean
                        .setPriority(generated.getPriority())
                        .setPreMatching(generated.isPreMatching())
                        .setNonBlockingRequired(generated.isNonBlocking())
                        .setReadBody(generated.isReadBody());
                if (!generated.getNameBindingNames().isEmpty()) {
                    builder.setNameBindingNames(generated.getNameBindingNames());
                }

                additionalContainerRequestFilters.produce(builder.build());
            } else {
                // the user class itself is made to be a bean as we want the user to be able to declare dependencies
                additionalBeans.addBeanClass(generated.getDeclaringClassName());
                ContainerResponseFilterBuildItem.Builder builder = new ContainerResponseFilterBuildItem.Builder(
                        generated.getGeneratedClassName())
                        .setRegisterAsBean(false)// it has already been made a bean
                        .setPriority(generated.getPriority());
                if (!generated.getNameBindingNames().isEmpty()) {
                    builder.setNameBindingNames(generated.getNameBindingNames());
                }
                additionalContainerResponseFilters.produce(builder.build());
            }
        }
        Set<MethodInfo> classLevelExceptionMappers = new HashSet<>(resourceScanningResultBuildItem
                .map(s -> s.getResult().getClassLevelExceptionMappers()).orElse(Collections.emptyList()));
        for (AnnotationInstance instance : index
                .getAnnotations(ResteasyReactiveDotNames.SERVER_EXCEPTION_MAPPER)) {
            if (instance.target().kind() != AnnotationTarget.Kind.METHOD) {
                continue;
            }
            MethodInfo methodInfo = instance.target().asMethod();
            if (classLevelExceptionMappers.contains(methodInfo)) { // methods annotated with @ServerExceptionMapper that exist inside a Resource Class are handled differently
                continue;
            }
            // the user class itself is made to be a bean as we want the user to be able to declare dependencies
            additionalBeans.addBeanClass(methodInfo.declaringClass().name().toString());
            Map<String, String> generatedClassNames = ServerExceptionMapperGenerator.generateGlobalMapper(methodInfo,
                    new GeneratedBeanGizmoAdaptor(generatedBean),
                    Set.of(HTTP_SERVER_REQUEST, HTTP_SERVER_RESPONSE, ROUTING_CONTEXT), Set.of(Unremovable.class.getName()));
            for (Map.Entry<String, String> entry : generatedClassNames.entrySet()) {
                ExceptionMapperBuildItem.Builder builder = new ExceptionMapperBuildItem.Builder(entry.getValue(),
                        entry.getKey()).setRegisterAsBean(false);// it has already been made a bean
                AnnotationValue priorityValue = instance.value("priority");
                if (priorityValue != null) {
                    builder.setPriority(priorityValue.asInt());
                }
                additionalExceptionMappers.produce(builder.build());
            }
        }

        additionalBean.produce(additionalBeans.setUnremovable().setDefaultScope(DotNames.SINGLETON).build());
    }

}
