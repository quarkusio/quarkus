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
package io.quarkus.resteasy.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.servlet.DispatcherType;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

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
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ProxyUnwrapperBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateConfigBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.jaxb.deployment.JaxbEnabledBuildItem;
import io.quarkus.resteasy.common.deployment.JaxrsProvidersToRegisterBuildItem;
import io.quarkus.resteasy.common.deployment.ResteasyDotNames;
import io.quarkus.resteasy.common.deployment.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.runtime.QuarkusInjectorFactory;
import io.quarkus.resteasy.runtime.ResteasyFilter;
import io.quarkus.resteasy.runtime.ResteasyTemplate;
import io.quarkus.resteasy.runtime.RolesFilterRegistrar;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.undertow.deployment.FilterBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;
import io.quarkus.undertow.deployment.ServletInitParamBuildItem;

/**
 * Processor that finds JAX-RS classes in the deployment
 *
 * @author Stuart Douglas
 */
public class ResteasyScanningProcessor {

    private static final String JAVAX_WS_RS_APPLICATION = Application.class.getName();
    private static final String JAX_RS_FILTER_NAME = JAVAX_WS_RS_APPLICATION;
    private static final String JAX_RS_SERVLET_NAME = JAVAX_WS_RS_APPLICATION;
    private static final String JAX_RS_APPLICATION_PARAMETER_NAME = "javax.ws.rs.Application";

    private static final DotName JSONB_ANNOTATION = DotName.createSimple("javax.json.bind.annotation.JsonbAnnotation");

    private static final Set<DotName> TYPES_IGNORED_FOR_REFLECTION = new HashSet<>(Arrays.asList(
            // javax.json
            DotName.createSimple("javax.json.JsonObject"),
            DotName.createSimple("javax.json.JsonArray"),

            // JAX-RS
            DotName.createSimple(Response.class.getName()),
            DotName.createSimple(AsyncResponse.class.getName()),

            // Vert-x
            DotName.createSimple("io.vertx.core.json.JsonArray"),
            DotName.createSimple("io.vertx.core.json.JsonObject")));

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

    private static final Logger log = Logger.getLogger("io.quarkus.resteasy");

    @BuildStep
    io.quarkus.resteasy.deployment.ResteasyJaxrsConfig exportConfig() {
        return new io.quarkus.resteasy.deployment.ResteasyJaxrsConfig(resteasyConfig.path);
    }

    @BuildStep
    SubstrateConfigBuildItem config() {
        return SubstrateConfigBuildItem.builder()
                .addResourceBundle("messages")
                .build();
    }

    @BuildStep
    public void build(
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            BuildProducer<SubstrateProxyDefinitionBuildItem> proxyDefinition,
            BuildProducer<SubstrateResourceBuildItem> resource,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeClasses,
            BuildProducer<FilterBuildItem> filterProducer,
            BuildProducer<ServletBuildItem> servletProducer,
            BuildProducer<ServletInitParamBuildItem> servletContextParams,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            CombinedIndexBuildItem combinedIndexBuildItem) throws Exception {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.RESTEASY));

        IndexView index = combinedIndexBuildItem.getIndex();

        resource.produce(new SubstrateResourceBuildItem("META-INF/services/javax.ws.rs.client.ClientBuilder"));

        Collection<AnnotationInstance> app = index.getAnnotations(ResteasyDotNames.APPLICATION_PATH);
        //@Context uses proxies for interface injection
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

        for (ClassInfo implementation : index.getAllKnownImplementors(ResteasyDotNames.DYNAMIC_FEATURE)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, implementation.name().toString()));
        }

        //currently we only examine the first class that is annotated with @ApplicationPath so best
        //fail if there the user code has multiple such annotations instead of surprising the user
        //at runtime
        if (app.size() > 1) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (AnnotationInstance annotationInstance : app) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(annotationInstance.target().asClass().name().toString());
            }
            throw new RuntimeException("Multiple classes ( " + sb.toString()
                    + ") have been annotated with @ApplicationPath which is currently not supported");
        }
        String mappingPath;
        String path = null;
        String appClass = null;
        if (!app.isEmpty()) {
            AnnotationInstance appPath = app.iterator().next();
            path = appPath.value().asString();
            appClass = appPath.target().asClass().name().toString();
        } else {
            path = resteasyConfig.path;
        }
        if (path.endsWith("/")) {
            mappingPath = path + "*";
        } else {
            mappingPath = path + "/*";
        }

        Collection<AnnotationInstance> paths = index.getAnnotations(ResteasyDotNames.PATH);
        if (paths != null && !paths.isEmpty()) {

            //if JAX-RS is installed at the root location we use a filter, otherwise we use a Servlet and take over the whole mapped path
            if (path.equals("/")) {
                filterProducer
                        .produce(FilterBuildItem.builder(JAX_RS_FILTER_NAME, ResteasyFilter.class.getName()).setLoadOnStartup(1)
                                .addFilterServletNameMapping("default", DispatcherType.REQUEST).setAsyncSupported(true)
                                .build());
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, ResteasyFilter.class.getName()));
            } else {
                servletProducer.produce(ServletBuildItem.builder(JAX_RS_SERVLET_NAME, HttpServlet30Dispatcher.class.getName())
                        .setLoadOnStartup(1).addMapping(mappingPath).setAsyncSupported(true).build());
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, HttpServlet30Dispatcher.class.getName()));
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

            if (!resources.isEmpty()) {
                servletContextParams.produce(
                        new ServletInitParamBuildItem(ResteasyContextParameters.RESTEASY_SCANNED_RESOURCES,
                                String.join(",", resources)));
            }
            servletContextParams.produce(new ServletInitParamBuildItem("resteasy.servlet.mapping.prefix", path));
            if (appClass != null) {
                servletContextParams.produce(new ServletInitParamBuildItem(JAX_RS_APPLICATION_PARAMETER_NAME, appClass));
            }

            // generate default constructors for suitable concrete @Path classes that don't have them
            // see https://issues.jboss.org/browse/RESTEASY-2183
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
        } else {
            // no @Application class and no detected @Path resources, bail out
            return;
        }

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

        registerReflectionForSerialization(reflectiveClass, reflectiveHierarchy, combinedIndexBuildItem);
    }

    @BuildStep
    void registerProviders(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ServletInitParamBuildItem> servletContextParams,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            JaxrsProvidersToRegisterBuildItem jaxrsProvidersToRegisterBuildItem) {

        if (jaxrsProvidersToRegisterBuildItem.useBuiltIn()) {
            // if we find a wildcard media type, we just use the built-in providers
            servletContextParams.produce(new ServletInitParamBuildItem("resteasy.use.builtin.providers", "true"));

            if (!jaxrsProvidersToRegisterBuildItem.getContributedProviders().isEmpty()) {
                servletContextParams.produce(new ServletInitParamBuildItem("resteasy.providers",
                        String.join(",", jaxrsProvidersToRegisterBuildItem.getContributedProviders())));
            }
        } else {
            servletContextParams.produce(new ServletInitParamBuildItem("resteasy.use.builtin.providers", "false"));
            servletContextParams.produce(new ServletInitParamBuildItem("resteasy.providers",
                    String.join(",", jaxrsProvidersToRegisterBuildItem.getProviders())));
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

    @Record(STATIC_INIT)
    @BuildStep
    void setupInjection(ResteasyTemplate template,
            BuildProducer<ServletInitParamBuildItem> servletContextParams,
            BeanContainerBuildItem beanContainerBuildItem,
            List<ProxyUnwrapperBuildItem> proxyUnwrappers) {

        List<Function<Object, Object>> unwrappers = new ArrayList<>();
        for (ProxyUnwrapperBuildItem i : proxyUnwrappers) {
            unwrappers.add(i.getUnwrapper());
        }
        template.setupIntegration(beanContainerBuildItem.getValue(), unwrappers);

        servletContextParams
                .produce(new ServletInitParamBuildItem("resteasy.injector.factory", QuarkusInjectorFactory.class.getName()));
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
     * Install the JAXRS security provider
     *
     * @param providers - the JaxrsProviderBuildItem providers producer to use
     */
    @BuildStep
    void setupFilter(BuildProducer<ResteasyJaxrsProviderBuildItem> providers) {
        providers.produce(new ResteasyJaxrsProviderBuildItem(RolesFilterRegistrar.class.getName()));
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

    private void registerReflectionForSerialization(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            CombinedIndexBuildItem combinedIndexBuildItem) {
        IndexView index = combinedIndexBuildItem.getIndex();

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
            Collection<AnnotationInstance> instances = index.getAnnotations(annotationType);
            for (AnnotationInstance instance : instances) {
                MethodInfo method = instance.target().asMethod();
                if (isReflectionDeclarationRequiredFor(method.returnType())) {
                    reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem(method.returnType()));
                }
                for (short i = 0; i < method.parameters().size(); i++) {
                    Type parameterType = method.parameters().get(i);
                    if (isReflectionDeclarationRequiredFor(parameterType)
                            && !hasAnnotation(method, i, ResteasyDotNames.CONTEXT)) {
                        reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem(parameterType));
                    }
                }
            }
        }

        // In the case of a constraint violation, these elements might be returned as entities and will be serialized
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, ViolationReport.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, ResteasyConstraintViolation.class.getName()));
    }

    private static boolean isReflectionDeclarationRequiredFor(Type type) {
        DotName className = getClassName(type);

        return className != null && !TYPES_IGNORED_FOR_REFLECTION.contains(className);
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
}
