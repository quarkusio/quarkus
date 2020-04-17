package io.quarkus.resteasy.server.common.deployment;

import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_TIME;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.servlet.DispatcherType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;
import org.jboss.resteasy.api.validation.ViolationReport;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.microprofile.config.FilterConfigSource;
import org.jboss.resteasy.microprofile.config.ServletConfigSource;
import org.jboss.resteasy.microprofile.config.ServletContextConfigSource;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.arc.ArcUndeclaredThrowableException;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.AutoInjectAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.CustomScopeAnnotationsBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassNameExclusion;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.Transformation;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.resteasy.common.deployment.JaxrsProvidersToRegisterBuildItem;
import io.quarkus.resteasy.common.deployment.ResteasyCommonProcessor.ResteasyCommonConfig;
import io.quarkus.resteasy.common.deployment.ResteasyDotNames;
import io.quarkus.resteasy.common.runtime.QuarkusInjectorFactory;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.server.common.spi.AdditionalJaxRsResourceDefiningAnnotationBuildItem;
import io.quarkus.resteasy.server.common.spi.AdditionalJaxRsResourceMethodAnnotationsBuildItem;
import io.quarkus.resteasy.server.common.spi.AdditionalJaxRsResourceMethodParamAnnotations;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.undertow.deployment.FilterBuildItem;

/**
 * Processor that builds the RESTEasy server configuration.
 */
public class ResteasyServerCommonProcessor {

    private static final Logger log = Logger.getLogger("io.quarkus.resteasy");

    private static final String JAX_RS_APPLICATION_PARAMETER_NAME = "javax.ws.rs.Application";

    private static final DotName JSONB_ANNOTATION = DotName.createSimple("javax.json.bind.annotation.JsonbAnnotation");

    private static final DotName[] METHOD_ANNOTATIONS = {
            ResteasyDotNames.GET,
            ResteasyDotNames.HEAD,
            ResteasyDotNames.DELETE,
            ResteasyDotNames.OPTIONS,
            ResteasyDotNames.PATCH,
            ResteasyDotNames.POST,
            ResteasyDotNames.PUT,
    };

    private static final DotName[] RESTEASY_PARAM_ANNOTATIONS = {
            ResteasyDotNames.RESTEASY_QUERY_PARAM,
            ResteasyDotNames.RESTEASY_FORM_PARAM,
            ResteasyDotNames.RESTEASY_COOKIE_PARAM,
            ResteasyDotNames.RESTEASY_PATH_PARAM,
            ResteasyDotNames.RESTEASY_HEADER_PARAM,
            ResteasyDotNames.RESTEASY_MATRIX_PARAM,
    };

    /**
     * JAX-RS configuration.
     */
    ResteasyConfig resteasyConfig;
    ResteasyCommonConfig commonConfig;

    @ConfigRoot(phase = BUILD_TIME)
    static final class ResteasyConfig {
        /**
         * If this is true then JAX-RS will use only a single instance of a resource
         * class to service all requests.
         * <p>
         * If this is false then it will create a new instance of the resource per
         * request.
         * <p>
         * If the resource class has an explicit CDI scope annotation then the value of
         * this annotation will always be used to control the lifecycle of the resource
         * class.
         * <p>
         * IMPLEMENTATION NOTE: {@code javax.ws.rs.Path} turns into a CDI stereotype
         * with singleton scope. As a result, if a user annotates a JAX-RS resource with
         * a stereotype which has a different default scope the deployment fails with
         * IllegalStateException.
         */
        @ConfigItem(defaultValue = "true")
        boolean singletonResources;

        /**
         * Set this to override the default path for JAX-RS resources if there are no
         * annotated application classes.
         */
        @ConfigItem(defaultValue = "/")
        String path;

        /**
         * Whether or not JAX-RS metrics should be enabled if the Metrics capability is present and Vert.x is being used.
         */
        @ConfigItem(name = "metrics.enabled", defaultValue = "false")
        public boolean metricsEnabled;

    }

    @BuildStep
    NativeImageConfigBuildItem config() {
        return NativeImageConfigBuildItem.builder()
                .addResourceBundle("messages")
                .build();
    }

    @BuildStep
    public void build(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinition,
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeClasses,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ResteasyServerConfigBuildItem> resteasyServerConfig,
            BuildProducer<ResteasyDeploymentBuildItem> resteasyDeployment,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer,
            List<AutoInjectAnnotationBuildItem> autoInjectAnnotations,
            List<AdditionalJaxRsResourceDefiningAnnotationBuildItem> additionalJaxRsResourceDefiningAnnotations,
            List<AdditionalJaxRsResourceMethodAnnotationsBuildItem> additionalJaxRsResourceMethodAnnotations,
            List<AdditionalJaxRsResourceMethodParamAnnotations> additionalJaxRsResourceMethodParamAnnotations,
            List<ResteasyDeploymentCustomizerBuildItem> deploymentCustomizers,
            JaxrsProvidersToRegisterBuildItem jaxrsProvidersToRegisterBuildItem,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            Optional<ResteasyServletMappingBuildItem> resteasyServletMappingBuildItem,
            CustomScopeAnnotationsBuildItem scopes) throws Exception {
        IndexView index = combinedIndexBuildItem.getIndex();

        resource.produce(new NativeImageResourceBuildItem("META-INF/services/javax.ws.rs.client.ClientBuilder"));

        Collection<AnnotationInstance> applicationPaths = index.getAnnotations(ResteasyDotNames.APPLICATION_PATH);

        // currently we only examine the first class that is annotated with @ApplicationPath so best
        // fail if the user code has multiple such annotations instead of surprising the user
        // at runtime
        if (applicationPaths.size() > 1) {
            throw createMultipleApplicationsException(applicationPaths);
        }

        Collection<AnnotationInstance> paths = beanArchiveIndexBuildItem.getIndex().getAnnotations(ResteasyDotNames.PATH);
        Set<AnnotationInstance> additionalPaths = new HashSet<>();
        for (AdditionalJaxRsResourceDefiningAnnotationBuildItem annotation : additionalJaxRsResourceDefiningAnnotations) {
            additionalPaths.addAll(beanArchiveIndexBuildItem.getIndex().getAnnotations(annotation.getAnnotationClass()));
        }

        Collection<AnnotationInstance> allPaths = new ArrayList<>(paths);
        allPaths.addAll(additionalPaths);

        if (allPaths.isEmpty()) {
            // no detected @Path, bail out
            return;
        }

        final String path;
        final String appClass;
        if (!applicationPaths.isEmpty()) {
            AnnotationInstance applicationPath = applicationPaths.iterator().next();
            path = applicationPath.value().asString();
            appClass = applicationPath.target().asClass().name().toString();
        } else {
            if (resteasyServletMappingBuildItem.isPresent()) {
                if (resteasyServletMappingBuildItem.get().getPath().endsWith("/*")) {
                    path = resteasyServletMappingBuildItem.get().getPath().substring(0,
                            resteasyServletMappingBuildItem.get().getPath().length() - 1);
                } else {
                    path = resteasyServletMappingBuildItem.get().getPath();
                }
                appClass = null;
            } else {
                path = resteasyConfig.path;
                appClass = null;
            }
        }

        Map<DotName, ClassInfo> scannedResources = new HashMap<>();
        Set<DotName> pathInterfaces = new HashSet<>();
        Map<DotName, ClassInfo> withoutDefaultCtor = new HashMap<>();
        for (AnnotationInstance annotation : allPaths) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                ClassInfo clazz = annotation.target().asClass();
                if (!Modifier.isInterface(clazz.flags())) {
                    if (!withoutDefaultCtor.containsKey(clazz.name())) {
                        String className = clazz.name().toString();
                        if (!additionalPaths.contains(annotation)) { // scanned resources only contains real JAX-RS resources
                            scannedResources.putIfAbsent(clazz.name(), clazz);
                        }
                        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, className));

                        if (!clazz.hasNoArgsConstructor()) {
                            withoutDefaultCtor.put(clazz.name(), clazz);
                        }
                    }
                } else {
                    pathInterfaces.add(clazz.name());
                }
            }
        }

        // look for all implementations of interfaces annotated @Path
        for (final DotName iface : pathInterfaces) {
            final Collection<ClassInfo> implementors = index.getAllKnownImplementors(iface);
            for (final ClassInfo implementor : implementors) {
                String className = implementor.name().toString();
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, className));
                scannedResources.putIfAbsent(implementor.name(), implementor);
            }
        }

        Set<DotName> subresources = findSubresources(beanArchiveIndexBuildItem.getIndex(), scannedResources);
        if (!subresources.isEmpty()) {
            for (DotName locator : subresources) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, locator.toString()));
            }
            // Sub-resource locators are unremovable beans
            unremovableBeans.produce(
                    new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanClassNamesExclusion(
                            subresources.stream().map(Object::toString).collect(Collectors.toSet()))));
        }

        // generate default constructors for suitable concrete @Path classes that don't have them
        // see https://issues.jboss.org/browse/RESTEASY-2183
        generateDefaultConstructors(transformers, withoutDefaultCtor, additionalJaxRsResourceDefiningAnnotations);

        checkParameterNames(beanArchiveIndexBuildItem.getIndex(), additionalJaxRsResourceMethodParamAnnotations);

        registerContextProxyDefinitions(beanArchiveIndexBuildItem.getIndex(), proxyDefinition);

        registerReflectionForSerialization(reflectiveClass, reflectiveHierarchy, combinedIndexBuildItem,
                beanArchiveIndexBuildItem, additionalJaxRsResourceMethodAnnotations);

        for (ClassInfo implementation : index.getAllKnownImplementors(ResteasyDotNames.DYNAMIC_FEATURE)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, implementation.name().toString()));
        }

        Map<String, String> resteasyInitParameters = new HashMap<>();

        ResteasyDeployment deployment = new ResteasyDeploymentImpl();
        registerProviders(deployment, resteasyInitParameters, reflectiveClass, unremovableBeans,
                jaxrsProvidersToRegisterBuildItem);

        if (!scannedResources.isEmpty()) {
            deployment.getScannedResourceClasses()
                    .addAll(scannedResources.keySet().stream().map(Object::toString).collect(Collectors.toList()));
            resteasyInitParameters.put(ResteasyContextParameters.RESTEASY_SCANNED_RESOURCES,
                    scannedResources.keySet().stream().map(Object::toString).collect(Collectors.joining(",")));
        }
        resteasyInitParameters.put(ResteasyContextParameters.RESTEASY_SERVLET_MAPPING_PREFIX, path);
        if (appClass != null) {
            deployment.setApplicationClass(appClass);
            resteasyInitParameters.put(JAX_RS_APPLICATION_PARAMETER_NAME, appClass);
        }
        resteasyInitParameters.put("resteasy.injector.factory", QuarkusInjectorFactory.class.getName());
        deployment.setInjectorFactoryClass(QuarkusInjectorFactory.class.getName());

        // apply all customizers
        for (ResteasyDeploymentCustomizerBuildItem deploymentCustomizer : deploymentCustomizers) {
            deploymentCustomizer.getConsumer().accept(deployment);
        }

        if (commonConfig.gzip.enabled) {
            resteasyInitParameters.put(ResteasyContextParameters.RESTEASY_GZIP_MAX_INPUT,
                    Long.toString(commonConfig.gzip.maxInput.asLongValue()));
        }
        resteasyInitParameters.put(ResteasyContextParameters.RESTEASY_UNWRAPPED_EXCEPTIONS,
                ArcUndeclaredThrowableException.class.getName());

        resteasyServerConfig.produce(new ResteasyServerConfigBuildItem(path, resteasyInitParameters));

        Set<DotName> autoInjectAnnotationNames = autoInjectAnnotations.stream().flatMap(a -> a.getAnnotationNames().stream())
                .collect(Collectors.toSet());
        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public boolean appliesTo(Kind kind) {
                return kind == Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext context) {
                ClassInfo clazz = context.getTarget().asClass();
                if (clazz.classAnnotation(ResteasyDotNames.PATH) != null) {
                    // Root resources - no need to add scope, @Path is a bean defining annotation
                    if (clazz.classAnnotation(DotNames.TYPED) == null) {
                        // Add @Typed(MyResource.class)
                        context.transform().add(createTypedAnnotationInstance(clazz)).done();
                    }
                    return;
                }
                if (scopes.isScopeIn(context.getAnnotations())) {
                    // Skip classes annotated with built-in scope
                    return;
                }
                if (clazz.classAnnotation(ResteasyDotNames.PROVIDER) != null) {
                    Transformation transformation = null;
                    if (clazz.annotations().containsKey(DotNames.INJECT)
                            || hasAutoInjectAnnotation(autoInjectAnnotationNames, clazz)) {
                        // A provider with an injection point but no built-in scope is @Singleton
                        transformation = context.transform().add(BuiltinScope.SINGLETON.getName());
                    }
                    if (clazz.classAnnotation(DotNames.TYPED) == null) {
                        // Add @Typed(MyProvider.class)
                        if (transformation == null) {
                            transformation = context.transform();
                        }
                        transformation.add(createTypedAnnotationInstance(clazz));
                    }
                    if (transformation != null) {
                        transformation.done();
                    }
                } else if (subresources.contains(clazz.name())) {
                    // Transform a class annotated with a request method designator
                    Transformation transformation = context.transform()
                            .add(resteasyConfig.singletonResources ? BuiltinScope.SINGLETON.getName()
                                    : BuiltinScope.DEPENDENT.getName());
                    if (clazz.classAnnotation(DotNames.TYPED) == null) {
                        // Add @Typed(MySubresource.class)
                        transformation.add(createTypedAnnotationInstance(clazz));
                    }
                    transformation.done();
                }
            }
        }));
        resteasyDeployment.produce(new ResteasyDeploymentBuildItem(path, deployment));
    }

    @BuildStep
    void processPathInterfaceImplementors(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            CustomScopeAnnotationsBuildItem scopes) {
        // NOTE: we cannot process @Path interface implementors within the ResteasyServerCommonProcessor.build() method because of build cycles
        IndexView index = combinedIndexBuildItem.getIndex();
        Set<DotName> pathInterfaces = new HashSet<>();
        for (AnnotationInstance annotation : index.getAnnotations(ResteasyDotNames.PATH)) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS
                    && Modifier.isInterface(annotation.target().asClass().flags())) {
                pathInterfaces.add(annotation.target().asClass().name());
            }
        }
        if (pathInterfaces.isEmpty()) {
            return;
        }
        Map<DotName, ClassInfo> pathInterfaceImplementors = new HashMap<>();
        for (DotName iface : pathInterfaces) {
            for (ClassInfo implementor : index.getAllKnownImplementors(iface)) {
                if (!pathInterfaceImplementors.containsKey(implementor.name())) {
                    pathInterfaceImplementors.put(implementor.name(), implementor);
                }
            }
        }
        if (!pathInterfaceImplementors.isEmpty()) {
            AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder()
                    .setDefaultScope(resteasyConfig.singletonResources ? BuiltinScope.SINGLETON.getName() : null)
                    .setUnremovable();
            for (Map.Entry<DotName, ClassInfo> implementor : pathInterfaceImplementors.entrySet()) {
                if (scopes.isScopeDeclaredOn(implementor.getValue())) {
                    // It has a scope defined - just mark it as unremovable
                    unremovableBeans
                            .produce(new UnremovableBeanBuildItem(new BeanClassNameExclusion(implementor.getKey().toString())));
                } else {
                    // No built-in scope found - add as additional bean
                    builder.addBeanClass(implementor.getKey().toString());
                }
            }
            additionalBeans.produce(builder.build());
        }
    }

    @BuildStep
    void beanDefiningAnnotations(BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(ResteasyDotNames.PATH,
                        resteasyConfig.singletonResources ? BuiltinScope.SINGLETON.getName() : null));
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(ResteasyDotNames.APPLICATION_PATH,
                        BuiltinScope.SINGLETON.getName()));
    }

    @BuildStep
    void enableMetrics(ResteasyConfig buildConfig,
            BuildProducer<ResteasyJaxrsProviderBuildItem> jaxRsProviders,
            BuildProducer<FilterBuildItem> servletFilters,
            Capabilities capabilities) {
        if (buildConfig.metricsEnabled && capabilities.isCapabilityPresent(Capabilities.METRICS)) {
            if (capabilities.isCapabilityPresent(Capabilities.SERVLET)) {
                // if running with servlet, use the MetricsFilter implementation from SmallRye
                jaxRsProviders.produce(
                        new ResteasyJaxrsProviderBuildItem("io.smallrye.metrics.jaxrs.JaxRsMetricsFilter"));
                servletFilters.produce(
                        FilterBuildItem.builder("metricsFilter", "io.smallrye.metrics.jaxrs.JaxRsMetricsServletFilter")
                                .setAsyncSupported(true)
                                .addFilterUrlMapping("*", DispatcherType.FORWARD)
                                .addFilterUrlMapping("*", DispatcherType.INCLUDE)
                                .addFilterUrlMapping("*", DispatcherType.REQUEST)
                                .addFilterUrlMapping("*", DispatcherType.ASYNC)
                                .addFilterUrlMapping("*", DispatcherType.ERROR)
                                .build());
            } else {
                // if running with vert.x, use the MetricsFilter implementation from Quarkus codebase
                jaxRsProviders.produce(
                        new ResteasyJaxrsProviderBuildItem("io.quarkus.smallrye.metrics.runtime.QuarkusJaxRsMetricsFilter"));
            }
        }
    }

    private boolean hasAutoInjectAnnotation(Set<DotName> autoInjectAnnotationNames, ClassInfo clazz) {
        for (DotName name : autoInjectAnnotationNames) {
            List<AnnotationInstance> instances = clazz.annotations().get(name);
            if (instances != null) {
                for (AnnotationInstance instance : instances) {
                    if (instance.target().kind() == Kind.FIELD) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private AnnotationInstance createTypedAnnotationInstance(ClassInfo clazz) {
        return AnnotationInstance.create(DotNames.TYPED, clazz,
                new AnnotationValue[] { AnnotationValue.createArrayValue("value",
                        new AnnotationValue[] { AnnotationValue.createClassValue("value",
                                Type.create(clazz.name(), org.jboss.jandex.Type.Kind.CLASS)) }) });
    }

    private Set<DotName> findSubresources(IndexView index, Map<DotName, ClassInfo> scannedResources) {
        // First identify sub-resource candidates
        Set<DotName> subresources = new HashSet<>();
        for (DotName annotation : METHOD_ANNOTATIONS) {
            Collection<AnnotationInstance> annotationInstances = index.getAnnotations(annotation);
            for (AnnotationInstance annotationInstance : annotationInstances) {
                DotName declaringClassName = annotationInstance.target().asMethod().declaringClass().name();
                if (scannedResources.containsKey(declaringClassName)) {
                    // Skip resource classes
                    continue;
                }
                subresources.add(declaringClassName);
            }
        }
        if (!subresources.isEmpty()) {
            // Collect sub-resource locator return types
            Set<DotName> subresourceLocatorTypes = new HashSet<>();
            for (ClassInfo resourceClass : scannedResources.values()) {
                ClassInfo clazz = resourceClass;
                while (clazz != null) {
                    for (MethodInfo method : clazz.methods()) {
                        if (method.hasAnnotation(ResteasyDotNames.PATH)) {
                            subresourceLocatorTypes.add(method.returnType().name());
                        }
                    }
                    if (clazz.superName().equals(DotNames.OBJECT)) {
                        clazz = null;
                    } else {
                        clazz = index.getClassByName(clazz.superName());
                    }
                }
            }
            // Remove false positives
            for (Iterator<DotName> iterator = subresources.iterator(); iterator.hasNext();) {
                DotName subresource = iterator.next();
                for (DotName type : subresourceLocatorTypes) {
                    // Sub-resource may be a subclass of a locator return type
                    if (!subresource.equals(type)
                            && index.getAllKnownSubclasses(type).stream().noneMatch(c -> c.name().equals(subresource))) {
                        iterator.remove();
                        break;
                    }
                }
            }
        }
        log.trace("Sub-resources found: " + subresources);
        return subresources;
    }

    private static void registerProviders(ResteasyDeployment deployment,
            Map<String, String> resteasyInitParameters,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            JaxrsProvidersToRegisterBuildItem jaxrsProvidersToRegisterBuildItem) {

        if (jaxrsProvidersToRegisterBuildItem.useBuiltIn()) {
            // if we find a wildcard media type, we just use the built-in providers
            resteasyInitParameters.put(ResteasyContextParameters.RESTEASY_USE_BUILTIN_PROVIDERS, "true");
            deployment.setRegisterBuiltin(true);

            if (!jaxrsProvidersToRegisterBuildItem.getContributedProviders().isEmpty()) {
                deployment.getProviderClasses().addAll(jaxrsProvidersToRegisterBuildItem.getContributedProviders());
                resteasyInitParameters.put(ResteasyContextParameters.RESTEASY_PROVIDERS,
                        String.join(",", jaxrsProvidersToRegisterBuildItem.getContributedProviders()));
            }
        } else {
            deployment.setRegisterBuiltin(false);
            deployment.getProviderClasses().addAll(jaxrsProvidersToRegisterBuildItem.getProviders());
            resteasyInitParameters.put(ResteasyContextParameters.RESTEASY_USE_BUILTIN_PROVIDERS, "false");
            resteasyInitParameters.put(ResteasyContextParameters.RESTEASY_PROVIDERS,
                    String.join(",", jaxrsProvidersToRegisterBuildItem.getProviders()));
        }

        // register the providers for reflection
        for (String providerToRegister : jaxrsProvidersToRegisterBuildItem.getProviders()) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, providerToRegister));
        }

        // special case: our config providers
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                ServletConfigSource.class,
                ServletContextConfigSource.class,
                FilterConfigSource.class));

        // Providers that are also beans are unremovable
        unremovableBeans.produce(new UnremovableBeanBuildItem(
                b -> jaxrsProvidersToRegisterBuildItem.getProviders().contains(b.getBeanClass().toString())));
    }

    private static void generateDefaultConstructors(BuildProducer<BytecodeTransformerBuildItem> transformers,
            Map<DotName, ClassInfo> withoutDefaultCtor,
            List<AdditionalJaxRsResourceDefiningAnnotationBuildItem> additionalJaxRsResourceDefiningAnnotations) {

        final Set<String> allowedAnnotationPrefixes = new HashSet<>(1 + additionalJaxRsResourceDefiningAnnotations.size());
        allowedAnnotationPrefixes.add(packageName(ResteasyDotNames.PATH));
        allowedAnnotationPrefixes.add("kotlin"); // make sure the annotation that the Kotlin compiler adds don't interfere with creating a default constructor
        allowedAnnotationPrefixes.add("io.quarkus.security"); // same for the security annotations
        allowedAnnotationPrefixes.add("javax.annotation.security");
        allowedAnnotationPrefixes.add("jakarta.annotation.security");
        for (AdditionalJaxRsResourceDefiningAnnotationBuildItem additionalJaxRsResourceDefiningAnnotation : additionalJaxRsResourceDefiningAnnotations) {
            final String packageName = packageName(additionalJaxRsResourceDefiningAnnotation.getAnnotationClass());
            if (packageName != null) {
                allowedAnnotationPrefixes.add(packageName);
            }
        }

        for (Map.Entry<DotName, ClassInfo> entry : withoutDefaultCtor.entrySet()) {
            final ClassInfo classInfo = entry.getValue();
            // keep it super simple - only generate default constructor is the object is a direct descendant of Object
            if (!(classInfo.superClassType() != null && classInfo.superClassType().name().equals(DotNames.OBJECT))) {
                return;
            }

            boolean hasNonJaxRSAnnotations = false;
            for (AnnotationInstance instance : classInfo.classAnnotations()) {
                final String packageName = packageName(instance.name());
                if (packageName == null || !allowedAnnotationPrefixes.contains(packageName)) {
                    hasNonJaxRSAnnotations = true;
                    break;
                }
            }

            // again keep it very very simple, if there are any non JAX-RS annotations, we don't generate the constructor
            if (hasNonJaxRSAnnotations) {
                continue;
            }

            final String name = classInfo.name().toString();
            transformers
                    .produce(new BytecodeTransformerBuildItem(name, new BiFunction<String, ClassVisitor, ClassVisitor>() {
                        @Override
                        public ClassVisitor apply(String className, ClassVisitor classVisitor) {
                            ClassVisitor cv = new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {

                                @Override
                                public void visit(int version, int access, String name, String signature, String superName,
                                        String[] interfaces) {
                                    super.visit(version, access, name, signature, superName, interfaces);
                                    MethodVisitor ctor = visitMethod(Modifier.PUBLIC, "<init>", "()V", null,
                                            null);
                                    ctor.visitCode();
                                    ctor.visitVarInsn(Opcodes.ALOAD, 0);
                                    ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
                                    ctor.visitInsn(Opcodes.RETURN);
                                    ctor.visitMaxs(1, 1);
                                    ctor.visitEnd();
                                }
                            };
                            return cv;
                        }
                    }));
        }
    }

    private static String packageName(DotName dotName) {
        final String className = dotName.toString();
        final int index = className.lastIndexOf('.');
        if (index > 0 && index < className.length() - 1) {
            return className.substring(0, index);
        }
        return null;
    }

    private static void checkParameterNames(IndexView index,
            List<AdditionalJaxRsResourceMethodParamAnnotations> additionalJaxRsResourceMethodParamAnnotations) {

        final List<DotName> methodParameterAnnotations = new ArrayList<>(RESTEASY_PARAM_ANNOTATIONS.length);
        methodParameterAnnotations.addAll(Arrays.asList(RESTEASY_PARAM_ANNOTATIONS));
        for (AdditionalJaxRsResourceMethodParamAnnotations annotations : additionalJaxRsResourceMethodParamAnnotations) {
            methodParameterAnnotations.addAll(annotations.getAnnotationClasses());
        }

        OUTER: for (DotName annotationType : methodParameterAnnotations) {
            Collection<AnnotationInstance> instances = index.getAnnotations(annotationType);
            for (AnnotationInstance instance : instances) {
                // we only care about method parameters, because properties or fields always work
                if (instance.target().kind() != Kind.METHOD_PARAMETER) {
                    continue;
                }
                MethodParameterInfo param = instance.target().asMethodParameter();
                if (param.name() == null) {
                    log.warnv(
                            "Detected RESTEasy annotation {0} on method parameter {1}.{2} with no name. Either specify its name,"
                                    + " or tell your compiler to enable debug info (-g) or parameter names (-parameters). This message is only"
                                    + " logged for the first such parameter.",
                            instance.name(),
                            param.method().declaringClass(), param.method().name());
                    break OUTER;
                }
            }
        }
    }

    private static void registerContextProxyDefinitions(IndexView index,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinition) {
        // @Context uses proxies for interface injection
        for (AnnotationInstance annotation : index.getAnnotations(ResteasyDotNames.CONTEXT)) {
            Type annotatedType = null;
            if (annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                MethodInfo method = annotation.target().asMethod();
                if (method.parameters().size() == 1) {
                    annotatedType = method.parameters().get(0);
                }
            } else if (annotation.target().kind() == AnnotationTarget.Kind.FIELD) {
                annotatedType = annotation.target().asField().type();
            } else if (annotation.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                int pos = annotation.target().asMethodParameter().position();
                annotatedType = annotation.target().asMethodParameter().method().parameters().get(pos);
            }
            if (annotatedType != null && annotatedType.kind() != Type.Kind.PRIMITIVE) {
                ClassInfo type = index.getClassByName(annotatedType.name());
                if (type != null) {
                    if (Modifier.isInterface(type.flags())) {
                        proxyDefinition.produce(new NativeImageProxyDefinitionBuildItem(type.toString()));
                    }
                } else {
                    //might be a framework class, which should be loadable
                    try {
                        Class<?> typeClass = Class.forName(annotatedType.name().toString());
                        if (typeClass.isInterface()) {
                            proxyDefinition.produce(new NativeImageProxyDefinitionBuildItem(annotatedType.name().toString()));
                        }
                    } catch (Exception e) {
                        //ignore
                    }
                }
            }
        }
    }

    private static void registerReflectionForSerialization(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            List<AdditionalJaxRsResourceMethodAnnotationsBuildItem> additionalJaxRsResourceMethodAnnotations) {
        IndexView index = combinedIndexBuildItem.getIndex();
        IndexView beanArchiveIndex = beanArchiveIndexBuildItem.getIndex();

        // This is probably redundant with the automatic resolution we do just below but better be safe
        for (AnnotationInstance annotation : index.getAnnotations(JSONB_ANNOTATION)) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                reflectiveClass
                        .produce(new ReflectiveClassBuildItem(true, true, annotation.target().asClass().name().toString()));
            }
        }

        final List<DotName> annotations = new ArrayList<>(METHOD_ANNOTATIONS.length);
        annotations.addAll(Arrays.asList(METHOD_ANNOTATIONS));
        for (AdditionalJaxRsResourceMethodAnnotationsBuildItem additionalJaxRsResourceMethodAnnotation : additionalJaxRsResourceMethodAnnotations) {
            annotations.addAll(additionalJaxRsResourceMethodAnnotation.getAnnotationClasses());
        }

        // Declare reflection for all the types implicated in the Rest end points (return types and parameters).
        // It might be needed for serialization.
        for (DotName annotationType : annotations) {
            scanMethodParameters(annotationType, reflectiveHierarchy, index);
            scanMethodParameters(annotationType, reflectiveHierarchy, beanArchiveIndex);
        }

        // In the case of a constraint violation, these elements might be returned as entities and will be serialized
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, ViolationReport.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, ResteasyConstraintViolation.class.getName()));
    }

    private static void scanMethodParameters(DotName annotationType,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy, IndexView index) {
        Collection<AnnotationInstance> instances = index.getAnnotations(annotationType);
        for (AnnotationInstance instance : instances) {
            if (instance.target().kind() != Kind.METHOD) {
                continue;
            }
            MethodInfo method = instance.target().asMethod();
            reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem(method.returnType(), index,
                    ResteasyDotNames.IGNORE_FOR_REFLECTION_PREDICATE));

            for (short i = 0; i < method.parameters().size(); i++) {
                Type parameterType = method.parameters().get(i);
                if (!hasAnnotation(method, i, ResteasyDotNames.CONTEXT)) {
                    reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem(parameterType, index,
                            ResteasyDotNames.IGNORE_FOR_REFLECTION_PREDICATE));
                }
            }
        }
    }

    private static boolean hasAnnotation(MethodInfo method, short paramPosition, DotName annotation) {
        for (AnnotationInstance annotationInstance : method.annotations()) {
            AnnotationTarget target = annotationInstance.target();
            if (target != null && target.kind() == Kind.METHOD_PARAMETER
                    && target.asMethodParameter().position() == paramPosition
                    && annotationInstance.name().equals(annotation)) {
                return true;
            }
        }
        return false;
    }

    private static RuntimeException createMultipleApplicationsException(Collection<AnnotationInstance> applicationPaths) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (AnnotationInstance annotationInstance : applicationPaths) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(annotationInstance.target().asClass().name().toString());
        }
        return new RuntimeException("Multiple classes ( " + sb.toString()
                + ") have been annotated with @ApplicationPath which is currently not supported");
    }
}
