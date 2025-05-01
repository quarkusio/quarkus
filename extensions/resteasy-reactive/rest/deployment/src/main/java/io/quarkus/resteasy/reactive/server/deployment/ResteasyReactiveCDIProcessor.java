package io.quarkus.resteasy.reactive.server.deployment;

import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.APPLICATION_PATH;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.CONTEXT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PATH;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PROVIDER;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.core.HttpHeaders;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Declaration;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.scanning.ResourceScanningResult;
import org.jboss.resteasy.reactive.server.injection.ContextProducers;
import org.jboss.resteasy.reactive.server.processor.util.ResteasyReactiveServerDotNames;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.AutoInjectAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.ResourceScanningResultBuildItem;
import io.quarkus.resteasy.reactive.server.runtime.QuarkusContextProducers;
import io.quarkus.resteasy.reactive.server.spi.SubResourcesAsBeansBuildItem;
import io.quarkus.resteasy.reactive.spi.DynamicFeatureBuildItem;
import io.quarkus.resteasy.reactive.spi.JaxrsFeatureBuildItem;

public class ResteasyReactiveCDIProcessor {

    @BuildStep
    AutoInjectAnnotationBuildItem contextInjection(
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeanBuildItemBuildProducer) {
        additionalBeanBuildItemBuildProducer
                .produce(AdditionalBeanBuildItem.builder()
                        .addBeanClasses(ContextProducers.class, QuarkusContextProducers.class)
                        .build());
        unremovableBeanBuildItemBuildProducer.produce(UnremovableBeanBuildItem.beanTypes(HttpHeaders.class));
        return new AutoInjectAnnotationBuildItem(ResteasyReactiveServerDotNames.CONTEXT,
                DotName.createSimple(BeanParam.class.getName()));
    }

    @BuildStep
    void beanDefiningAnnotations(BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(PATH, BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(APPLICATION_PATH,
                        BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(PROVIDER,
                        BuiltinScope.SINGLETON.getName()));
    }

    /**
     * The idea here is to make a best effort to find resources that need to be {@link RequestScoped}
     * and make them such if no scope has been defined.
     * If any other scope has been explicitly defined, the build will fail
     */
    @BuildStep
    void requestScopedResources(Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            BuildProducer<AnnotationsTransformerBuildItem> additionalBeanBuildItemBuildProducer) {
        if (resourceScanningResultBuildItem.isEmpty()) {
            return;
        }
        Set<DotName> requestScopedResources = resourceScanningResultBuildItem.get().getResult()
                .getRequestScopedResources();

        additionalBeanBuildItemBuildProducer.produce(new io.quarkus.arc.deployment.AnnotationsTransformerBuildItem(
                AnnotationTransformation.builder().whenDeclaration(
                        new Predicate<>() {
                            @Override
                            public boolean test(Declaration declaration) {
                                return declaration.kind() == AnnotationTarget.Kind.CLASS;
                            }
                        }).transform(new Consumer<>() {
                            @Override
                            public void accept(AnnotationTransformation.TransformationContext context) {
                                if (context.declaration().kind() == AnnotationTarget.Kind.CLASS) {
                                    ClassInfo clazz = context.declaration().asClass();
                                    if (requestScopedResources.contains(clazz.name())) {
                                        BuiltinScope builtinScope = BuiltinScope.from(clazz);
                                        if (builtinScope != null) {
                                            if (builtinScope.getName() != BuiltinScope.REQUEST.getName()) {
                                                throw new DeploymentException(
                                                        "Resource classes that use field injection for REST parameters can only be @RequestScoped. Offending class is "
                                                                + clazz.name());
                                            } else {
                                                // nothing to do as @RequestScoped was already present
                                            }
                                        } else if (!resourceScanningResultBuildItem.get().getResult().getPossibleSubResources()
                                                .containsKey(clazz.name())) {
                                            // no @RequestScoped for Sub Resources, since they might have a constructor
                                            // not compatible with CDI. User must explicitly mark as RequestScoped
                                            context.add(RequestScoped.class);
                                        }
                                    }
                                }
                            }
                        })));
    }

    @BuildStep
    void unremovableContextMethodParams(Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            BuildProducer<UnremovableBeanBuildItem> producer) {

        if (resourceScanningResultBuildItem.isEmpty()) {
            return;
        }

        Collection<ClassInfo> resourceClasses = resourceScanningResultBuildItem.get().getResult().getScannedResources()
                .values();
        for (ClassInfo resourceClass : resourceClasses) {
            if (resourceClass.annotationsMap().containsKey(CONTEXT)) {
                for (AnnotationInstance instance : resourceClass.annotationsMap().get(CONTEXT)) {
                    if (instance.target().kind() != AnnotationTarget.Kind.METHOD_PARAMETER) {
                        continue;
                    }
                    producer.produce(UnremovableBeanBuildItem
                            .beanTypes(instance.target().asMethodParameter().type().name()));
                }
            }
        }
    }

    @BuildStep
    void perClassExceptionMapperSupport(Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            BuildProducer<AnnotationsTransformerBuildItem> producer) {
        if (resourceScanningResultBuildItem.isEmpty()) {
            return;
        }
        List<MethodInfo> methodExceptionMapper = resourceScanningResultBuildItem.get().getResult()
                .getClassLevelExceptionMappers();
        if (methodExceptionMapper.isEmpty()) {
            return;
        }
        producer.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == AnnotationTarget.Kind.METHOD;
            }

            @Override
            public void transform(TransformationContext ctx) {
                final Collection<AnnotationInstance> annotations = ctx.getAnnotations();
                AnnotationInstance serverExceptionMapper = Annotations.find(annotations,
                        ResteasyReactiveDotNames.SERVER_EXCEPTION_MAPPER);
                if (serverExceptionMapper == null) {
                    return;
                }
                // we want to make sure that class level exception mappers do not run
                // interceptors bound using class-level interceptor bindings (like security)
                ctx.transform().add(DotNames.NO_CLASS_INTERCEPTORS).done();
            }
        }));
    }

    @BuildStep
    void subResourcesAsBeans(ResourceScanningResultBuildItem setupEndpointsResult,
            List<SubResourcesAsBeansBuildItem> subResourcesAsBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableProducer,
            BuildProducer<AdditionalBeanBuildItem> additionalProducer) {
        Map<DotName, ClassInfo> possibleSubResources = setupEndpointsResult.getResult().getPossibleSubResources();
        if (possibleSubResources.isEmpty()) {
            return;
        }

        // make SubResources unremovable - this will only apply if they become beans by some other means
        unremovableProducer.produce(UnremovableBeanBuildItem.beanTypes(possibleSubResources.keySet()));

        if (subResourcesAsBeans.isEmpty()) {
            return;
        }

        // now actually make SubResources beans as it was requested via build item
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        for (DotName subResourceClass : possibleSubResources.keySet()) {
            builder.addBeanClass(subResourceClass.toString());
        }
        additionalProducer.produce(builder.build());
    }

    // when an interface is annotated with @Path and there is only one implementation of it that is not annotated with @Path,
    // we need to make this class a bean. See https://github.com/quarkusio/quarkus/issues/15028
    @BuildStep
    void pathInterfaceImpls(Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer) {
        if (!resourceScanningResultBuildItem.isPresent()) {
            return;
        }
        ResourceScanningResult resourceScanningResult = resourceScanningResultBuildItem.get().getResult();
        Map<DotName, String> pathInterfaces = resourceScanningResult.getPathInterfaces();
        List<String> impls = new ArrayList<>();
        for (Map.Entry<DotName, String> i : pathInterfaces.entrySet()) {
            List<ClassInfo> candidateBeans = new ArrayList<>(1);
            for (ClassInfo clazz : resourceScanningResult.getIndex().getAllKnownImplementors(i.getKey())) {
                if (!Modifier.isAbstract(clazz.flags())) {
                    if ((clazz.enclosingClass() == null || Modifier.isStatic(clazz.flags())) &&
                            clazz.enclosingMethod() == null) {
                        candidateBeans.add(clazz);
                    }
                }
            }
            if (candidateBeans.size() == 1) {
                impls.add(candidateBeans.get(0).name().toString());
            }
        }
        if (!impls.isEmpty()) {
            additionalBeanBuildItemBuildProducer
                    .produce(AdditionalBeanBuildItem.builder().setUnremovable().addBeanClasses(impls.toArray(new String[0]))
                            .build());
        }
    }

    @BuildStep
    void additionalBeans(List<DynamicFeatureBuildItem> additionalDynamicFeatures,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer,
            List<JaxrsFeatureBuildItem> featureBuildItems,
            BuildProducer<AdditionalBeanBuildItem> additionalBean) {

        AdditionalBeanBuildItem.Builder additionalProviders = AdditionalBeanBuildItem.builder();
        for (DynamicFeatureBuildItem dynamicFeature : additionalDynamicFeatures) {
            if (dynamicFeature.isRegisterAsBean()) {
                additionalProviders.addBeanClass(dynamicFeature.getClassName());
            } else {
                reflectiveClassBuildItemBuildProducer
                        .produce(ReflectiveClassBuildItem.builder(dynamicFeature.getClassName())
                                .build());
            }
        }
        for (JaxrsFeatureBuildItem feature : featureBuildItems) {
            if (feature.isRegisterAsBean()) {
                additionalProviders.addBeanClass(feature.getClassName());
            } else {
                reflectiveClassBuildItemBuildProducer
                        .produce(ReflectiveClassBuildItem.builder(feature.getClassName())
                                .build());
            }
        }
        additionalBean.produce(additionalProviders.setUnremovable().setDefaultScope(DotNames.SINGLETON).build());
    }

}
