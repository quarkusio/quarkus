/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.resteasy.server.common.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
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
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassNameExclusion;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ProxyUnwrapperBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateConfigBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.jaxb.deployment.JaxbEnabledBuildItem;
import io.quarkus.resteasy.common.deployment.JaxrsProvidersToRegisterBuildItem;
import io.quarkus.resteasy.common.deployment.ResteasyCommonProcessor.ResteasyCommonConfig;
import io.quarkus.resteasy.common.deployment.ResteasyDotNames;
import io.quarkus.resteasy.server.common.runtime.QuarkusInjectorFactory;
import io.quarkus.resteasy.server.common.runtime.ResteasyServerCommonTemplate;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

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

    @ConfigRoot
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
    }

    @BuildStep
    SubstrateConfigBuildItem config() {
        return SubstrateConfigBuildItem.builder()
                .addResourceBundle("messages")
                .build();
    }

    @BuildStep
    public void build(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            BuildProducer<SubstrateProxyDefinitionBuildItem> proxyDefinition,
            BuildProducer<SubstrateResourceBuildItem> resource,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeClasses,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ResteasyServerConfigBuildItem> resteasyServerConfig,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            JaxrsProvidersToRegisterBuildItem jaxrsProvidersToRegisterBuildItem,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem) throws Exception {
        IndexView index = combinedIndexBuildItem.getIndex();

        resource.produce(new SubstrateResourceBuildItem("META-INF/services/javax.ws.rs.client.ClientBuilder"));

        Collection<AnnotationInstance> applicationPaths = index.getAnnotations(ResteasyDotNames.APPLICATION_PATH);

        // currently we only examine the first class that is annotated with @ApplicationPath so best
        // fail if the user code has multiple such annotations instead of surprising the user
        // at runtime
        if (applicationPaths.size() > 1) {
            throw createMultipleApplicationsException(applicationPaths);
        }

        Collection<AnnotationInstance> paths = beanArchiveIndexBuildItem.getIndex().getAnnotations(ResteasyDotNames.PATH);

        if (paths.isEmpty()) {
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
            path = resteasyConfig.path;
            appClass = null;
        }

        Set<String> resources = new HashSet<>();
        Set<DotName> pathInterfaces = new HashSet<>();
        Set<ClassInfo> withoutDefaultCtor = new HashSet<>();
        for (AnnotationInstance annotation : paths) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                ClassInfo clazz = annotation.target().asClass();
                if (!Modifier.isInterface(clazz.flags())) {
                    String className = clazz.name().toString();
                    resources.add(className);
                    reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, className));

                    if (!clazz.hasNoArgsConstructor()) {
                        withoutDefaultCtor.add(clazz);
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
                resources.add(className);
            }
        }

        // generate default constructors for suitable concrete @Path classes that don't have them
        // see https://issues.jboss.org/browse/RESTEASY-2183
        generateDefaultConstructors(transformers, withoutDefaultCtor);

        checkParameterNames(index);

        registerContextProxyDefinitions(index, proxyDefinition);

        registerReflectionForSerialization(reflectiveClass, reflectiveHierarchy, combinedIndexBuildItem,
                beanArchiveIndexBuildItem);

        for (ClassInfo implementation : index.getAllKnownImplementors(ResteasyDotNames.DYNAMIC_FEATURE)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, implementation.name().toString()));
        }

        Map<String, String> resteasyInitParameters = new HashMap<>();

        registerProviders(resteasyInitParameters, reflectiveClass, unremovableBeans, jaxrsProvidersToRegisterBuildItem);

        if (!resources.isEmpty()) {
            resteasyInitParameters.put(ResteasyContextParameters.RESTEASY_SCANNED_RESOURCES, String.join(",", resources));
        }
        resteasyInitParameters.put(ResteasyContextParameters.RESTEASY_SERVLET_MAPPING_PREFIX, path);
        if (appClass != null) {
            resteasyInitParameters.put(JAX_RS_APPLICATION_PARAMETER_NAME, appClass);
        }
        resteasyInitParameters.put("resteasy.injector.factory", QuarkusInjectorFactory.class.getName());

        if (commonConfig.gzip.enabled && commonConfig.gzip.maxInput.isPresent()) {
            resteasyInitParameters.put(ResteasyContextParameters.RESTEASY_GZIP_MAX_INPUT,
                    Integer.toString(commonConfig.gzip.maxInput.getAsInt()));
        }

        resteasyServerConfig.produce(new ResteasyServerConfigBuildItem(path, resteasyInitParameters));
    }

    @BuildStep
    void processPathInterfaceImplementors(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
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
        Set<ClassInfo> pathInterfaceImplementors = new HashSet<>();
        for (DotName iface : pathInterfaces) {
            for (ClassInfo implementor : index.getAllKnownImplementors(iface)) {
                pathInterfaceImplementors.add(implementor);
            }
        }
        if (!pathInterfaceImplementors.isEmpty()) {
            AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder()
                    .setDefaultScope(resteasyConfig.singletonResources ? BuiltinScope.SINGLETON.getName() : null)
                    .setUnremovable();
            for (ClassInfo implementor : pathInterfaceImplementors) {
                if (BuiltinScope.isDeclaredOn(implementor)) {
                    // It has a built-in scope - just mark it as unremovable
                    unremovableBeans
                            .produce(new UnremovableBeanBuildItem(new BeanClassNameExclusion(implementor.name().toString())));
                } else {
                    // No built-in scope found - add as additional bean
                    builder.addBeanClass(implementor.name().toString());
                }
            }
            additionalBeans.produce(builder.build());
        }
    }

    @Record(STATIC_INIT)
    @BuildStep
    ResteasyInjectionReadyBuildItem setupInjection(ResteasyServerCommonTemplate template,
            BeanContainerBuildItem beanContainerBuildItem,
            List<ProxyUnwrapperBuildItem> proxyUnwrappers) {
        List<Function<Object, Object>> unwrappers = new ArrayList<>();
        for (ProxyUnwrapperBuildItem i : proxyUnwrappers) {
            unwrappers.add(i.getUnwrapper());
        }
        template.setupIntegration(beanContainerBuildItem.getValue(), unwrappers);

        return new ResteasyInjectionReadyBuildItem();
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
    AnnotationsTransformerBuildItem annotationTransformer() {
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public boolean appliesTo(Kind kind) {
                return kind == Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext transformationContext) {
                ClassInfo clazz = transformationContext.getTarget().asClass();
                if (clazz.classAnnotation(ResteasyDotNames.PROVIDER) != null && clazz.annotations().containsKey(DotNames.INJECT)
                        && !BuiltinScope.isIn(clazz.classAnnotations())) {
                    // A provider with an injection point but no built-in scope is @Singleton
                    transformationContext.transform().add(BuiltinScope.SINGLETON.getName()).done();
                }
            }
        });
    }

    /**
     * Indicates that JAXB support should be enabled
     *
     * @return
     */
    @BuildStep
    JaxbEnabledBuildItem enableJaxb() {
        return new JaxbEnabledBuildItem();
    }

    private static void registerProviders(Map<String, String> resteasyInitParameters,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            JaxrsProvidersToRegisterBuildItem jaxrsProvidersToRegisterBuildItem) {

        if (jaxrsProvidersToRegisterBuildItem.useBuiltIn()) {
            // if we find a wildcard media type, we just use the built-in providers
            resteasyInitParameters.put(ResteasyContextParameters.RESTEASY_USE_BUILTIN_PROVIDERS, "true");

            if (!jaxrsProvidersToRegisterBuildItem.getContributedProviders().isEmpty()) {
                resteasyInitParameters.put(ResteasyContextParameters.RESTEASY_PROVIDERS,
                        String.join(",", jaxrsProvidersToRegisterBuildItem.getContributedProviders()));
            }
        } else {
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
            Set<ClassInfo> withoutDefaultCtor) {
        for (ClassInfo classInfo : withoutDefaultCtor) {
            // keep it super simple - only generate default constructor is the object is a direct descendant of Object
            if (!(classInfo.superClassType() != null && classInfo.superClassType().name().equals(DotNames.OBJECT))) {
                return;
            }

            boolean hasNonJaxRSAnnotations = false;
            for (AnnotationInstance instance : classInfo.classAnnotations()) {
                if (!instance.name().toString().startsWith("javax.ws.rs")) {
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
                            ClassVisitor cv = new ClassVisitor(Opcodes.ASM6, classVisitor) {

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

    private static void checkParameterNames(IndexView index) {
        OUTER: for (DotName annotationType : RESTEASY_PARAM_ANNOTATIONS) {
            Collection<AnnotationInstance> instances = index.getAnnotations(annotationType);
            for (AnnotationInstance instance : instances) {
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
            BuildProducer<SubstrateProxyDefinitionBuildItem> proxyDefinition) {
        // @Context uses proxies for interface injection
        for (AnnotationInstance annotation : index.getAnnotations(ResteasyDotNames.CONTEXT)) {
            DotName typeName = null;
            if (annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                MethodInfo method = annotation.target().asMethod();
                if (method.parameters().size() == 1) {
                    typeName = method.parameters().get(0).name();
                }
            } else if (annotation.target().kind() == AnnotationTarget.Kind.FIELD) {
                typeName = annotation.target().asField().type().name();
            } else if (annotation.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                int pos = annotation.target().asMethodParameter().position();
                typeName = annotation.target().asMethodParameter().method().parameters().get(pos).name();
            }
            if (typeName != null) {
                ClassInfo type = index.getClassByName(typeName);
                if (type != null) {
                    if (Modifier.isInterface(type.flags())) {
                        proxyDefinition.produce(new SubstrateProxyDefinitionBuildItem(type.toString()));
                    }
                } else {
                    //might be a framework class, which should be loadable
                    try {
                        Class<?> typeClass = Class.forName(typeName.toString());
                        if (typeClass.isInterface()) {
                            proxyDefinition.produce(new SubstrateProxyDefinitionBuildItem(typeName.toString()));
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
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem) {
        IndexView index = combinedIndexBuildItem.getIndex();
        IndexView beanArchiveIndex = beanArchiveIndexBuildItem.getIndex();

        // required by Jackson
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false,
                "com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector",
                "com.fasterxml.jackson.databind.ser.std.SqlDateSerializer"));

        // This is probably redundant with the automatic resolution we do just below but better be safe
        for (AnnotationInstance annotation : index.getAnnotations(JSONB_ANNOTATION)) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                reflectiveClass
                        .produce(new ReflectiveClassBuildItem(true, true, annotation.target().asClass().name().toString()));
            }
        }

        // Declare reflection for all the types implicated in the Rest end points (return types and parameters).
        // It might be needed for serialization.
        for (DotName annotationType : METHOD_ANNOTATIONS) {
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
            MethodInfo method = instance.target().asMethod();
            if (isReflectionDeclarationRequiredFor(method.returnType())) {
                reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem(method.returnType(), index));
            }
            for (short i = 0; i < method.parameters().size(); i++) {
                Type parameterType = method.parameters().get(i);
                if (isReflectionDeclarationRequiredFor(parameterType)
                        && !hasAnnotation(method, i, ResteasyDotNames.CONTEXT)) {
                    reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem(parameterType, index));
                }
            }
        }
    }

    private static boolean isReflectionDeclarationRequiredFor(Type type) {
        DotName className = getClassName(type);

        return className != null && !ResteasyDotNames.TYPES_IGNORED_FOR_REFLECTION.contains(className);
    }

    private static DotName getClassName(Type type) {
        switch (type.kind()) {
            case CLASS:
            case PARAMETERIZED_TYPE:
                return type.name();
            case ARRAY:
                return getClassName(type.asArrayType().component());
            default:
                return null;
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
