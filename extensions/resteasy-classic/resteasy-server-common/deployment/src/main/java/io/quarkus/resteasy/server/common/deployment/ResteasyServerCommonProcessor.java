package io.quarkus.resteasy.server.common.deployment;

import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_TIME;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.ws.rs.core.Application;

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
import io.quarkus.arc.deployment.BuildTimeConditionBuildItem;
import io.quarkus.arc.deployment.CustomScopeAnnotationsBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassNameExclusion;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.Transformation;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.resteasy.common.deployment.JaxrsProvidersToRegisterBuildItem;
import io.quarkus.resteasy.common.deployment.ResteasyCommonProcessor.ResteasyCommonConfig;
import io.quarkus.resteasy.common.runtime.QuarkusInjectorFactory;
import io.quarkus.resteasy.common.spi.ResteasyDotNames;
import io.quarkus.resteasy.server.common.runtime.QuarkusResteasyDeployment;
import io.quarkus.resteasy.server.common.spi.AdditionalJaxRsResourceDefiningAnnotationBuildItem;
import io.quarkus.resteasy.server.common.spi.AdditionalJaxRsResourceMethodAnnotationsBuildItem;
import io.quarkus.resteasy.server.common.spi.AdditionalJaxRsResourceMethodParamAnnotations;
import io.quarkus.resteasy.server.common.spi.AllowedJaxRsAnnotationPrefixBuildItem;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Processor that builds the RESTEasy server configuration.
 */
public class ResteasyServerCommonProcessor {

    private static final Logger log = Logger.getLogger("io.quarkus.resteasy");

    private static final String JAX_RS_APPLICATION_PARAMETER_NAME = "javax.ws.rs.Application";

    private static final DotName JSONB_ANNOTATION = DotName.createSimple("javax.json.bind.annotation.JsonbAnnotation");

    private static final List<DotName> METHOD_ANNOTATIONS = List.of(
            ResteasyDotNames.GET,
            ResteasyDotNames.HEAD,
            ResteasyDotNames.DELETE,
            ResteasyDotNames.OPTIONS,
            ResteasyDotNames.PATCH,
            ResteasyDotNames.POST,
            ResteasyDotNames.PUT);

    private static final List<DotName> RESTEASY_PARAM_ANNOTATIONS = List.of(
            ResteasyDotNames.RESTEASY_QUERY_PARAM,
            ResteasyDotNames.RESTEASY_FORM_PARAM,
            ResteasyDotNames.RESTEASY_COOKIE_PARAM,
            ResteasyDotNames.RESTEASY_PATH_PARAM,
            ResteasyDotNames.RESTEASY_HEADER_PARAM,
            ResteasyDotNames.RESTEASY_MATRIX_PARAM);

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
         * Whether or not detailed JAX-RS metrics should be enabled if the smallrye-metrics
         * extension is present.
         * <p>
         * See <a href=
         * "https://github.com/eclipse/microprofile-metrics/blob/2.3.x/spec/src/main/asciidoc/required-metrics.adoc#optional-rest">MicroProfile
         * Metrics: Optional REST metrics</a>.
         * <p>
         * Deprecated. Use {@code quarkus.smallrye-metrics.jaxrs.enabled}.
         */
        @ConfigItem(name = "metrics.enabled", defaultValue = "false")
        public boolean metricsEnabled;

        /**
         * Ignore all explicit JAX-RS {@link Application} classes.
         * As multiple JAX-RS applications are not supported, this can be used to effectively merge all JAX-RS applications.
         */
        @ConfigItem(defaultValue = "false")
        boolean ignoreApplicationClasses;

        /**
         * Whether or not annotations such `@IfBuildTimeProfile`, `@IfBuildTimeProperty` and friends will be taken
         * into account when used on JAX-RS classes.
         */
        @ConfigItem(defaultValue = "true")
        boolean buildTimeConditionAware;
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
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ResteasyServerConfigBuildItem> resteasyServerConfig,
            BuildProducer<ResteasyDeploymentBuildItem> resteasyDeployment,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer,
            List<BuildTimeConditionBuildItem> buildTimeConditions,
            List<AutoInjectAnnotationBuildItem> autoInjectAnnotations,
            List<AdditionalJaxRsResourceDefiningAnnotationBuildItem> additionalJaxRsResourceDefiningAnnotations,
            List<AdditionalJaxRsResourceMethodAnnotationsBuildItem> additionalJaxRsResourceMethodAnnotations,
            List<AdditionalJaxRsResourceMethodParamAnnotations> additionalJaxRsResourceMethodParamAnnotations,
            List<AllowedJaxRsAnnotationPrefixBuildItem> friendlyJaxRsAnnotationPrefixes,
            List<ResteasyDeploymentCustomizerBuildItem> deploymentCustomizers,
            JaxrsProvidersToRegisterBuildItem jaxrsProvidersToRegisterBuildItem,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            Optional<ResteasyServletMappingBuildItem> resteasyServletMappingBuildItem,
            CustomScopeAnnotationsBuildItem scopes) throws Exception {
        IndexView index = combinedIndexBuildItem.getIndex();

        Collection<AnnotationInstance> applicationPaths = Collections.emptySet();
        final Set<String> allowedClasses;
        final Set<String> excludedClasses;
        if (resteasyConfig.buildTimeConditionAware) {
            excludedClasses = getExcludedClasses(buildTimeConditions);
        } else {
            excludedClasses = Collections.emptySet();
        }
        if (resteasyConfig.ignoreApplicationClasses) {
            allowedClasses = Collections.emptySet();
        } else {
            applicationPaths = index.getAnnotations(ResteasyDotNames.APPLICATION_PATH);
            allowedClasses = getAllowedClasses(index);
            jaxrsProvidersToRegisterBuildItem = getFilteredJaxrsProvidersToRegisterBuildItem(
                    jaxrsProvidersToRegisterBuildItem, allowedClasses, excludedClasses);
        }

        boolean filterClasses = !allowedClasses.isEmpty() || !excludedClasses.isEmpty();

        // currently we only examine the first class that is annotated with @ApplicationPath so best
        // fail if the user code has multiple such annotations instead of surprising the user
        // at runtime
        if (applicationPaths.size() > 1) {
            throw createMultipleApplicationsException(applicationPaths);
        }

        Set<AnnotationInstance> additionalPaths = new HashSet<>();
        for (AdditionalJaxRsResourceDefiningAnnotationBuildItem annotation : additionalJaxRsResourceDefiningAnnotations) {
            additionalPaths.addAll(beanArchiveIndexBuildItem.getIndex().getAnnotations(annotation.getAnnotationClass()));
        }

        Collection<AnnotationInstance> paths = beanArchiveIndexBuildItem.getIndex().getAnnotations(ResteasyDotNames.PATH);
        final Collection<AnnotationInstance> allPaths;
        if (filterClasses) {
            allPaths = paths.stream().filter(
                    annotationInstance -> keepAnnotation(beanArchiveIndexBuildItem.getIndex(), allowedClasses, excludedClasses,
                            annotationInstance))
                    .collect(Collectors.toList());
        } else {
            allPaths = new ArrayList<>(paths);
        }
        allPaths.addAll(additionalPaths);

        if (allPaths.isEmpty()) {
            // no detected @Path, bail out
            return;
        }

        final String rootPath;
        final String path;
        final String appClass;
        if (!applicationPaths.isEmpty()) {
            AnnotationInstance applicationPath = applicationPaths.iterator().next();
            rootPath = "/";
            path = applicationPath.value().asString();
            appClass = applicationPath.target().asClass().name().toString();
        } else {
            if (resteasyServletMappingBuildItem.isPresent()) {
                if (resteasyServletMappingBuildItem.get().getPath().endsWith("/*")) {
                    rootPath = resteasyServletMappingBuildItem.get().getPath().substring(0,
                            resteasyServletMappingBuildItem.get().getPath().length() - 1);
                } else {
                    rootPath = resteasyServletMappingBuildItem.get().getPath();
                }
                path = rootPath;
                appClass = null;
            } else {
                rootPath = resteasyConfig.path;
                path = resteasyConfig.path;
                appClass = null;
            }
        }

        Map<DotName, ClassInfo> scannedResources = new HashMap<>();
        Set<DotName> pathInterfaces = new HashSet<>();
        Set<DotName> pathAbstract = new HashSet<>();
        Map<DotName, ClassInfo> withoutDefaultCtor = new HashMap<>();
        for (AnnotationInstance annotation : allPaths) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                ClassInfo clazz = annotation.target().asClass();
                if (!Modifier.isInterface(clazz.flags())) {
                    if (!withoutDefaultCtor.containsKey(clazz.name())) {
                        String className = clazz.name().toString();
                        if (!additionalPaths.contains(annotation)) { // scanned resources only contains real JAX-RS resources
                            if (Modifier.isAbstract(clazz.flags())) {
                                pathAbstract.add(clazz.name());
                            } else {
                                scannedResources.putIfAbsent(clazz.name(), clazz);
                            }
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

                if (!implementor.hasNoArgsConstructor()) {
                    withoutDefaultCtor.put(implementor.name(), implementor);
                }
            }
        }
        // look for all implementations of abstract classes annotated @Path
        for (final DotName cls : pathAbstract) {
            final Collection<ClassInfo> implementors = index.getAllKnownSubclasses(cls);
            for (final ClassInfo implementor : implementors) {
                String className = implementor.name().toString();
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, className));
                if (!Modifier.isAbstract(implementor.flags())) {
                    scannedResources.putIfAbsent(implementor.name(), implementor);
                }

                if (!implementor.hasNoArgsConstructor()) {
                    withoutDefaultCtor.put(implementor.name(), implementor);
                }
            }
        }

        // look for all annotated providers with no default constructor
        for (final String cls : jaxrsProvidersToRegisterBuildItem.getAnnotatedProviders()) {
            final ClassInfo info = index.getClassByName(DotName.createSimple(cls));
            if (info != null && !info.hasNoArgsConstructor()) {
                withoutDefaultCtor.put(info.name(), info);
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
        generateDefaultConstructors(transformers, withoutDefaultCtor, additionalJaxRsResourceDefiningAnnotations,
                friendlyJaxRsAnnotationPrefixes);

        checkParameterNames(beanArchiveIndexBuildItem.getIndex(), additionalJaxRsResourceMethodParamAnnotations);

        registerContextProxyDefinitions(beanArchiveIndexBuildItem.getIndex(), proxyDefinition);

        registerReflectionForSerialization(reflectiveClass, reflectiveHierarchy, combinedIndexBuildItem,
                beanArchiveIndexBuildItem, additionalJaxRsResourceMethodAnnotations);

        for (ClassInfo implementation : index.getAllKnownImplementors(ResteasyDotNames.DYNAMIC_FEATURE)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, implementation.name().toString()));
        }

        Map<String, String> resteasyInitParameters = new HashMap<>();

        ResteasyDeployment deployment = new QuarkusResteasyDeployment();
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

        resteasyServerConfig.produce(new ResteasyServerConfigBuildItem(rootPath, path, resteasyInitParameters));

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
    List<AllowedJaxRsAnnotationPrefixBuildItem> registerCompatibleAnnotationPrefixes() {
        List<AllowedJaxRsAnnotationPrefixBuildItem> prefixes = new ArrayList<>();
        prefixes.add(new AllowedJaxRsAnnotationPrefixBuildItem(packageName(ResteasyDotNames.PATH)));
        prefixes.add(new AllowedJaxRsAnnotationPrefixBuildItem("kotlin")); // make sure the annotation that the Kotlin compiler adds don't interfere with creating a default constructor
        prefixes.add(new AllowedJaxRsAnnotationPrefixBuildItem("lombok")); // same for lombok
        prefixes.add(new AllowedJaxRsAnnotationPrefixBuildItem("io.quarkus.security")); // same for the security annotations
        prefixes.add(new AllowedJaxRsAnnotationPrefixBuildItem("javax.annotation.security"));
        prefixes.add(new AllowedJaxRsAnnotationPrefixBuildItem("jakarta.annotation.security"));
        prefixes.add(new AllowedJaxRsAnnotationPrefixBuildItem("java.lang"));
        prefixes.add(new AllowedJaxRsAnnotationPrefixBuildItem("javax.inject"));
        return prefixes;
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
            List<AdditionalJaxRsResourceDefiningAnnotationBuildItem> additionalJaxRsResourceDefiningAnnotations,
            List<AllowedJaxRsAnnotationPrefixBuildItem> friendlyJaxRsAnnotationPrefixes) {

        final Set<String> allowedAnnotationPrefixes = new HashSet<>(1 + additionalJaxRsResourceDefiningAnnotations.size());
        friendlyJaxRsAnnotationPrefixes.stream()
                .map(prefix -> prefix.getAnnotationPrefix())
                .forEachOrdered(allowedAnnotationPrefixes::add);

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
                if (packageName == null || !isPackageAllowed(allowedAnnotationPrefixes, packageName)) {
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
                    .produce(new BytecodeTransformerBuildItem(true, name, new BiFunction<String, ClassVisitor, ClassVisitor>() {
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

    private static boolean isPackageAllowed(Set<String> allowedAnnotationPrefixes, String packageName) {
        return allowedAnnotationPrefixes.stream().anyMatch(prefix -> packageName.startsWith(prefix));
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

        final List<DotName> methodParameterAnnotations = new ArrayList<>(
                RESTEASY_PARAM_ANNOTATIONS.size() + additionalJaxRsResourceMethodParamAnnotations.size());
        methodParameterAnnotations.addAll(RESTEASY_PARAM_ANNOTATIONS);
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
                        Class<?> typeClass = Class.forName(annotatedType.name().toString(), false,
                                Thread.currentThread().getContextClassLoader());
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

        final List<DotName> annotations = new ArrayList<>(
                METHOD_ANNOTATIONS.size() + additionalJaxRsResourceMethodAnnotations.size());
        annotations.addAll(METHOD_ANNOTATIONS);
        for (AdditionalJaxRsResourceMethodAnnotationsBuildItem additionalJaxRsResourceMethodAnnotation : additionalJaxRsResourceMethodAnnotations) {
            annotations.addAll(additionalJaxRsResourceMethodAnnotation.getAnnotationClasses());
        }

        // Declare reflection for all the types implicated in the Rest end points (return types and parameters).
        // It might be needed for serialization.
        for (DotName annotationType : annotations) {
            Set<AnnotationInstance> processedAnnotations = new HashSet<>();
            scanMethods(annotationType, reflectiveHierarchy, beanArchiveIndex, processedAnnotations);
            scanMethods(annotationType, reflectiveHierarchy, index, processedAnnotations);
        }

        // In the case of a constraint violation, these elements might be returned as entities and will be serialized
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, ViolationReport.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, ResteasyConstraintViolation.class.getName()));
    }

    private static void scanMethods(DotName annotationType,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy, IndexView index,
            Set<AnnotationInstance> processedAnnotations) {
        Collection<AnnotationInstance> instances = index.getAnnotations(annotationType);
        for (AnnotationInstance instance : instances) {
            if (instance.target().kind() != Kind.METHOD) {
                continue;
            }
            if (processedAnnotations.contains(instance)) {
                continue;
            }
            processedAnnotations.add(instance);
            MethodInfo method = instance.target().asMethod();
            String source = ResteasyServerCommonProcessor.class.getSimpleName() + " > " + method.declaringClass() + "[" + method
                    + "]";

            reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem.Builder()
                    .type(method.returnType())
                    .index(index)
                    .ignoreTypePredicate(ResteasyDotNames.IGNORE_TYPE_FOR_REFLECTION_PREDICATE)
                    .ignoreFieldPredicate(ResteasyDotNames.IGNORE_FIELD_FOR_REFLECTION_PREDICATE)
                    .ignoreMethodPredicate(ResteasyDotNames.IGNORE_METHOD_FOR_REFLECTION_PREDICATE)
                    .source(source)
                    .build());

            for (short i = 0; i < method.parameters().size(); i++) {
                Type parameterType = method.parameters().get(i);
                if (!hasAnnotation(method, i, ResteasyDotNames.CONTEXT)) {
                    reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem.Builder()
                            .type(parameterType)
                            .index(index)
                            .ignoreTypePredicate(ResteasyDotNames.IGNORE_TYPE_FOR_REFLECTION_PREDICATE)
                            .ignoreFieldPredicate(ResteasyDotNames.IGNORE_FIELD_FOR_REFLECTION_PREDICATE)
                            .ignoreMethodPredicate(ResteasyDotNames.IGNORE_METHOD_FOR_REFLECTION_PREDICATE)
                            .source(source)
                            .build());
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

    /**
     * @param buildTimeConditions the build time conditions from which the excluded classes are extracted.
     * @return the set of classes that have been annotated with unsuccessful build time conditions.
     */
    private static Set<String> getExcludedClasses(List<BuildTimeConditionBuildItem> buildTimeConditions) {
        return buildTimeConditions.stream()
                .filter(item -> !item.isEnabled())
                .map(BuildTimeConditionBuildItem::getTarget)
                .filter(target -> target.kind() == Kind.CLASS)
                .map(target -> target.asClass().toString())
                .collect(Collectors.toSet());
    }

    /**
     * @param index the Jandex index view from which the class information is extracted.
     * @param allowedClasses the classes to keep provided by the methods {@link Application#getClasses()} and
     *        {@link Application#getSingletons()}.
     * @param excludedClasses the classes that have been annotated with unsuccessful build time conditions and that
     *        need to be excluded from the list of paths.
     * @param annotationInstance the annotation instance to test.
     * @return {@code true} if the enclosing class of the annotation is a concrete class and is part of the allowed
     *         classes, or is an interface and at least one concrete implementation is included, or is an abstract class
     *         and at least one concrete sub class is included, or is not part of the excluded classes, {@code false} otherwise.
     */
    private static boolean keepAnnotation(IndexView index, Set<String> allowedClasses, Set<String> excludedClasses,
            AnnotationInstance annotationInstance) {
        final ClassInfo classInfo = JandexUtil.getEnclosingClass(annotationInstance);
        final String className = classInfo.toString();
        if (allowedClasses.isEmpty()) {
            // No allowed classes have been set, meaning that only excluded classes have been provided.
            // Keep the enclosing class only if not excluded
            return !excludedClasses.contains(className);
        } else if (Modifier.isAbstract(classInfo.flags())) {
            // Only keep the annotation if a concrete implementation or a sub class has been included
            return (Modifier.isInterface(classInfo.flags()) ? index.getAllKnownImplementors(classInfo.name())
                    : index.getAllKnownSubclasses(classInfo.name()))
                            .stream()
                            .filter(clazz -> !Modifier.isAbstract(clazz.flags()))
                            .map(Objects::toString)
                            .anyMatch(allowedClasses::contains);
        }
        return allowedClasses.contains(className);
    }

    /**
     * @param allowedClasses the classes returned by the methods {@link Application#getClasses()} and
     *        {@link Application#getSingletons()} to keep.
     * @param excludedClasses the classes that have been annotated wih unsuccessful build time conditions and that
     *        need to be excluded from the list of providers.
     * @param jaxrsProvidersToRegisterBuildItem the initial {@code jaxrsProvidersToRegisterBuildItem} before being
     *        filtered
     * @return an instance of {@link JaxrsProvidersToRegisterBuildItem} that has been filtered to take into account
     *         the classes returned by the methods {@link Application#getClasses()} and {@link Application#getSingletons()}
     *         if at least one of those methods return a non empty {@code Set}, the provided instance of
     *         {@link JaxrsProvidersToRegisterBuildItem} otherwise.
     */
    private static JaxrsProvidersToRegisterBuildItem getFilteredJaxrsProvidersToRegisterBuildItem(
            JaxrsProvidersToRegisterBuildItem jaxrsProvidersToRegisterBuildItem, Set<String> allowedClasses,
            Set<String> excludedClasses) {

        if (allowedClasses.isEmpty() && excludedClasses.isEmpty()) {
            return jaxrsProvidersToRegisterBuildItem;
        }
        Set<String> providers = new HashSet<>(jaxrsProvidersToRegisterBuildItem.getProviders());
        Set<String> contributedProviders = new HashSet<>(jaxrsProvidersToRegisterBuildItem.getContributedProviders());
        Set<String> annotatedProviders = new HashSet<>(jaxrsProvidersToRegisterBuildItem.getAnnotatedProviders());
        providers.removeAll(annotatedProviders);
        contributedProviders.removeAll(annotatedProviders);
        if (allowedClasses.isEmpty()) {
            annotatedProviders.removeAll(excludedClasses);
        } else {
            annotatedProviders.retainAll(allowedClasses);
        }
        providers.addAll(annotatedProviders);
        contributedProviders.addAll(annotatedProviders);
        return new JaxrsProvidersToRegisterBuildItem(
                providers, contributedProviders, annotatedProviders, jaxrsProvidersToRegisterBuildItem.useBuiltIn());
    }

    /**
     * @param index the index to use to find the existing {@link Application}.
     * @return the set of classes returned by the methods {@link Application#getClasses()} and
     *         {@link Application#getSingletons()}.
     */
    private static Set<String> getAllowedClasses(IndexView index) {
        final Collection<ClassInfo> applications = index.getAllKnownSubclasses(ResteasyDotNames.APPLICATION);
        final Set<String> allowedClasses = new HashSet<>();
        Application application;
        ClassInfo selectedAppClass = null;
        for (ClassInfo applicationClassInfo : applications) {
            if (Modifier.isAbstract(applicationClassInfo.flags())) {
                continue;
            }
            if (selectedAppClass != null) {
                throw new RuntimeException("More than one Application class: " + applications);
            }
            selectedAppClass = applicationClassInfo;
            // FIXME: yell if there's more than one
            String applicationClass = applicationClassInfo.name().toString();
            try {
                Class<?> appClass = Thread.currentThread().getContextClassLoader().loadClass(applicationClass);
                application = (Application) appClass.getConstructor().newInstance();
                Set<Class<?>> classes = application.getClasses();
                if (!classes.isEmpty()) {
                    for (Class<?> klass : classes) {
                        allowedClasses.add(klass.getName());
                    }
                }
                classes = application.getSingletons().stream().map(Object::getClass).collect(Collectors.toSet());
                if (!classes.isEmpty()) {
                    for (Class<?> klass : classes) {
                        allowedClasses.add(klass.getName());
                    }
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException
                    | InvocationTargetException e) {
                throw new RuntimeException("Unable to handle class: " + applicationClass, e);
            }
        }
        return allowedClasses;
    }
}
