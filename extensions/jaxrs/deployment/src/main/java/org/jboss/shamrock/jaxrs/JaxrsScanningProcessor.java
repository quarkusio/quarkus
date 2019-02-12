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
package org.jboss.shamrock.jaxrs;

import static org.jboss.shamrock.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Singleton;
import javax.servlet.DispatcherType;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;
import org.jboss.resteasy.api.validation.ViolationReport;
import org.jboss.resteasy.core.MediaTypeMap;
import org.jboss.resteasy.plugins.interceptors.AcceptEncodingGZIPFilter;
import org.jboss.resteasy.plugins.interceptors.GZIPDecodingInterceptor;
import org.jboss.resteasy.plugins.interceptors.GZIPEncodingInterceptor;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.shamrock.arc.deployment.BeanContainerBuildItem;
import org.jboss.shamrock.arc.deployment.BeanDefiningAnnotationBuildItem;
import org.jboss.shamrock.deployment.annotations.BuildProducer;
import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.annotations.Record;
import org.jboss.shamrock.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.FeatureBuildItem;
import org.jboss.shamrock.deployment.builditem.ProxyUnwrapperBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveHierarchyBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateConfigBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateProxyDefinitionBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateResourceBuildItem;
import org.jboss.shamrock.deployment.util.ServiceUtil;
import org.jboss.shamrock.jaxrs.runtime.JaxrsTemplate;
import org.jboss.shamrock.jaxrs.runtime.ResteasyFilter;
import org.jboss.shamrock.jaxrs.runtime.RolesFilterRegistrar;
import org.jboss.shamrock.jaxrs.runtime.ShamrockInjectorFactory;
import org.jboss.shamrock.runtime.annotations.ConfigItem;
import org.jboss.shamrock.runtime.annotations.ConfigRoot;
import org.jboss.shamrock.undertow.FilterBuildItem;
import org.jboss.shamrock.undertow.ServletBuildItem;
import org.jboss.shamrock.undertow.ServletInitParamBuildItem;

/**
 * Processor that finds JAX-RS classes in the deployment
 *
 * @author Stuart Douglas
 */
public class JaxrsScanningProcessor {

    private static final String JAVAX_WS_RS_APPLICATION = "javax.ws.rs.Application";
    private static final String JAX_RS_FILTER_NAME = JAVAX_WS_RS_APPLICATION;
    private static final String JAX_RS_SERVLET_NAME = JAVAX_WS_RS_APPLICATION;
    private static final String JAX_RS_APPLICATION_PARAMETER_NAME = JAVAX_WS_RS_APPLICATION;

    private static final DotName APPLICATION_PATH = DotName.createSimple(ApplicationPath.class.getName());

    private static final DotName PATH = DotName.createSimple(Path.class.getName());
    private static final DotName PROVIDER = DotName.createSimple(Provider.class.getName());
    private static final DotName DYNAMIC_FEATURE = DotName.createSimple(DynamicFeature.class.getName());
    private static final DotName CONTEXT = DotName.createSimple(Context.class.getName());

    private static final DotName GET = DotName.createSimple(javax.ws.rs.GET.class.getName());
    private static final DotName HEAD = DotName.createSimple(javax.ws.rs.HEAD.class.getName());
    private static final DotName DELETE = DotName.createSimple(javax.ws.rs.DELETE.class.getName());
    private static final DotName OPTIONS = DotName.createSimple(javax.ws.rs.OPTIONS.class.getName());
    private static final DotName PATCH = DotName.createSimple(javax.ws.rs.PATCH.class.getName());
    private static final DotName POST = DotName.createSimple(javax.ws.rs.POST.class.getName());
    private static final DotName PUT = DotName.createSimple(javax.ws.rs.PUT.class.getName());

    private static final DotName CONSUMES = DotName.createSimple(Consumes.class.getName());
    private static final DotName PRODUCES = DotName.createSimple(Produces.class.getName());

    private static final DotName RESTEASY_QUERY_PARAM = DotName.createSimple(org.jboss.resteasy.annotations.jaxrs.QueryParam.class.getName());
    private static final DotName RESTEASY_FORM_PARAM = DotName.createSimple(org.jboss.resteasy.annotations.jaxrs.FormParam.class.getName());
    private static final DotName RESTEASY_COOKIE_PARAM = DotName.createSimple(org.jboss.resteasy.annotations.jaxrs.CookieParam.class.getName());
    private static final DotName RESTEASY_PATH_PARAM = DotName.createSimple(org.jboss.resteasy.annotations.jaxrs.PathParam.class.getName());
    private static final DotName RESTEASY_HEADER_PARAM = DotName.createSimple(org.jboss.resteasy.annotations.jaxrs.HeaderParam.class.getName());
    private static final DotName RESTEASY_MATRIX_PARAM = DotName.createSimple(org.jboss.resteasy.annotations.jaxrs.MatrixParam.class.getName());

    private static final DotName XML_ROOT = DotName.createSimple("javax.xml.bind.annotation.XmlRootElement");
    private static final DotName JSONB_ANNOTATION = DotName.createSimple("javax.json.bind.annotation.JsonbAnnotation");

    private static final Set<DotName> TYPES_IGNORED_FOR_REFLECTION = new HashSet<>(Arrays.asList(
            DotName.createSimple("javax.json.JsonObject"),
            DotName.createSimple("javax.json.JsonArray")
    ));

    private static final DotName[] METHOD_ANNOTATIONS = {
            GET,
            HEAD,
            DELETE,
            OPTIONS,
            PATCH,
            POST,
            PUT,
    };

    private static final DotName[] RESTEASY_PARAM_ANNOTATIONS = {
            RESTEASY_QUERY_PARAM,
            RESTEASY_FORM_PARAM,
            RESTEASY_COOKIE_PARAM,
            RESTEASY_PATH_PARAM,
            RESTEASY_HEADER_PARAM,
            RESTEASY_MATRIX_PARAM,
    };

    private static final ProviderDiscoverer[] PROVIDER_DISCOVERERS = {
            new ProviderDiscoverer(GET, false, true),
            new ProviderDiscoverer(HEAD, false, false),
            new ProviderDiscoverer(DELETE, true, false),
            new ProviderDiscoverer(OPTIONS, false, true),
            new ProviderDiscoverer(PATCH, true, false),
            new ProviderDiscoverer(POST, true, true),
            new ProviderDiscoverer(PUT, true, false)
    };
    private static final DotName SINGLETON_SCOPE = DotName.createSimple(Singleton.class.getName());

    /**
     * JAX-RS configuration.
     */
    JaxrsConfig jaxrs;

    @ConfigRoot
    static final class JaxrsConfig {
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
         * Enable gzip support for JAX-RS services.
         */
        @ConfigItem
        boolean enableGzip;

        /**
         * Set this to override the default path for JAX-RS resources if there are no
         * annotated application classes.
         */
        @ConfigItem(defaultValue = "/")
        String path;
    }

    private static final Logger log = Logger.getLogger("org.jboss.shamrock.jaxrs");

    @BuildStep
    org.jboss.shamrock.jaxrs.JaxrsConfig exportConfig() {
        return new org.jboss.shamrock.jaxrs.JaxrsConfig(jaxrs.path);
    }

    @BuildStep
    SubstrateConfigBuildItem config() {
        return SubstrateConfigBuildItem.builder()
                .addResourceBundle("messages")
                .addNativeImageSystemProperty("com.sun.xml.internal.bind.v2.bytecode.ClassTailor.noOptimize", "true") //com.sun.xml.internal.bind.v2.runtime.reflect.opt.AccessorInjector will attempt to use code that does not work if this is not set
                .build();
    }

    @BuildStep
    void scanForProviders(BuildProducer<JaxrsProviderBuildItem> providers, CombinedIndexBuildItem indexBuildItem) {
        for(AnnotationInstance i : indexBuildItem.getIndex().getAnnotations(PROVIDER)) {
            if(i.target().kind() == AnnotationTarget.Kind.CLASS) {
                providers.produce(new JaxrsProviderBuildItem(i.target().asClass().name().toString()));
            }
        }
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
                      CombinedIndexBuildItem combinedIndexBuildItem
    ) throws Exception {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.JAXRS));

        IndexView index = combinedIndexBuildItem.getIndex();

        resource.produce(new SubstrateResourceBuildItem("META-INF/services/javax.ws.rs.client.ClientBuilder"));

        Collection<AnnotationInstance> app = index.getAnnotations(APPLICATION_PATH);
        Collection<AnnotationInstance> xmlRoot = index.getAnnotations(XML_ROOT);
        if (!xmlRoot.isEmpty()) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, "com.sun.xml.bind.v2.ContextFactory",
                    "com.sun.xml.internal.bind.v2.ContextFactory"));
        }
        runtimeClasses.produce(new RuntimeInitializedClassBuildItem("com.sun.xml.internal.bind.v2.runtime.reflect.opt.Injector"));

        //@Context uses proxies for interface injection
        for (AnnotationInstance annotation : index.getAnnotations(CONTEXT)) {
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

        for (ClassInfo implementation : index.getAllKnownImplementors(DYNAMIC_FEATURE)) {
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
            throw new RuntimeException("Multiple classes ( "+ sb.toString() + ") have been annotated with @ApplicationPath which is currently not supported");
        }
        String mappingPath;
        String path = null;
        String appClass = null;
        if(!app.isEmpty()) {
            AnnotationInstance appPath = app.iterator().next();
            path = appPath.value().asString();
            appClass = appPath.target().asClass().name().toString();
        } else {
            path = jaxrs.path;
        }
        if (path.endsWith("/")) {
            mappingPath = path + "*";
        } else {
            mappingPath = path + "/*";
        }

        Collection<AnnotationInstance> paths = index.getAnnotations(PATH);
        if (paths != null && !paths.isEmpty()) {

            //if JAX-RS is installed at the root location we use a filter, otherwise we use a Servlet and take over the whole mapped path
            if (path.equals("/")) {
                filterProducer.produce(new FilterBuildItem(JAX_RS_FILTER_NAME, ResteasyFilter.class.getName()).setLoadOnStartup(1).addFilterServletNameMapping("default", DispatcherType.REQUEST).setAsyncSupported(true));
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, ResteasyFilter.class.getName()));
            } else {
                servletProducer.produce(new ServletBuildItem(JAX_RS_SERVLET_NAME, HttpServlet30Dispatcher.class.getName()).setLoadOnStartup(1).addMapping(mappingPath).setAsyncSupported(true));
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, HttpServlet30Dispatcher.class.getName()));
            }

            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (AnnotationInstance annotation : paths) {
                if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                    ClassInfo clazz = annotation.target().asClass();
                    if (!Modifier.isInterface(clazz.flags())) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(",");
                        }
                        String className = clazz.name().toString();
                        sb.append(className);
                        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, className));
                    }
                }
            }

            if (sb.length() > 0) {
                servletContextParams.produce(new ServletInitParamBuildItem(ResteasyContextParameters.RESTEASY_SCANNED_RESOURCES, sb.toString()));
            }
            servletContextParams.produce(new ServletInitParamBuildItem("resteasy.servlet.mapping.prefix", path));
            if (appClass != null) {
                servletContextParams.produce(new ServletInitParamBuildItem(JAX_RS_APPLICATION_PARAMETER_NAME, appClass));
            }
        } else {
            // no @Application class and no detected @Path resources, bail out
            return;
        }

        OUTER:
        for (DotName annotationType : RESTEASY_PARAM_ANNOTATIONS) {
            Collection<AnnotationInstance> instances = index.getAnnotations(annotationType);
            for (AnnotationInstance instance : instances) {
                MethodParameterInfo param = instance.target().asMethodParameter();
                if(param.name() == null) {
                    log.warnv("Detected RESTEasy annotation {0} on method parameter {1}.{2} with no name. Either specify its name,"
                             +" or tell your compiler to enable debug info (-g) or parameter names (-parameters). This message is only"
                            +" logged for the first such parameter.", instance.name(),
                             param.method().declaringClass(), param.method().name());
                    break OUTER;
                }
            }
        }

        registerReflectionForSerialization(reflectiveClass, reflectiveHierarchy, combinedIndexBuildItem);
    }

    @Record(STATIC_INIT)
    @BuildStep
    void registerProviders(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
                           BuildProducer<ServletInitParamBuildItem> servletContextParams,
                           CombinedIndexBuildItem combinedIndexBuildItem,
                           List<JaxrsProviderBuildItem> contributedProviderBuildItems) throws Exception {
        IndexView index = combinedIndexBuildItem.getIndex();

        Set<String> contributedProviders = new HashSet<>();
        for (JaxrsProviderBuildItem contributedProviderBuildItem : contributedProviderBuildItems) {
            contributedProviders.add(contributedProviderBuildItem.getName());
        }

        Set<String> availableProviders = ServiceUtil.classNamesNamedIn(getClass().getClassLoader(), "META-INF/services/" + Providers.class.getName());

        MediaTypeMap<String> categorizedReaders = new MediaTypeMap<>();
        MediaTypeMap<String> categorizedWriters = new MediaTypeMap<>();
        MediaTypeMap<String> categorizedContextResolvers = new MediaTypeMap<>();
        Set<String> otherProviders = new HashSet<>();

        categorizeProviders(availableProviders, categorizedReaders, categorizedWriters, categorizedContextResolvers, otherProviders);

        Set<String> providersToRegister = new HashSet<>();

        // add the other providers detected
        providersToRegister.addAll(otherProviders);

        // find the providers declared in our services
        boolean useBuiltinProviders = collectDeclaredProviders(providersToRegister, categorizedReaders, categorizedWriters, categorizedContextResolvers, index);

        // If GZIP support is enabled, enable it
        if (jaxrs.enableGzip) {
            providersToRegister.add(AcceptEncodingGZIPFilter.class.getName());
            providersToRegister.add(GZIPDecodingInterceptor.class.getName());
            providersToRegister.add(GZIPEncodingInterceptor.class.getName());
        }

        if (useBuiltinProviders) {
            // if we find a wildcard media type, we just use the built-in providers
            servletContextParams.produce(new ServletInitParamBuildItem("resteasy.use.builtin.providers", "true"));
            if (!contributedProviders.isEmpty()) {
                servletContextParams.produce(new ServletInitParamBuildItem("resteasy.providers", contributedProviders.stream().collect(Collectors.joining(","))));
            }

            providersToRegister = new HashSet<>(contributedProviders);
            providersToRegister.addAll(availableProviders);
        } else {
            providersToRegister.addAll(contributedProviders);
            servletContextParams.produce(new ServletInitParamBuildItem("resteasy.use.builtin.providers", "false"));
            servletContextParams.produce(new ServletInitParamBuildItem("resteasy.providers", providersToRegister.stream().collect(Collectors.joining(","))));
        }

        // register the providers for reflection
        for (String providerToRegister : providersToRegister) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, providerToRegister));
        }
    }

    @Record(STATIC_INIT)
    @BuildStep
    void setupInjection(JaxrsTemplate template,
            BuildProducer<ServletInitParamBuildItem> servletContextParams,
            BeanContainerBuildItem beanContainerBuildItem,
            List<ProxyUnwrapperBuildItem> proxyUnwrappers) {

        List<Function<Object, Object>> unwrappers = new ArrayList<>();
        for (ProxyUnwrapperBuildItem i : proxyUnwrappers) {
            unwrappers.add(i.getUnwrapper());
        }
        template.setupIntegration(beanContainerBuildItem.getValue(), unwrappers);

        servletContextParams.produce(new ServletInitParamBuildItem("resteasy.injector.factory", ShamrockInjectorFactory.class.getName()));
    }


    @BuildStep
    List<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations() {
        return Collections.singletonList(new BeanDefiningAnnotationBuildItem(PATH, jaxrs.singletonResources ? SINGLETON_SCOPE : null));
    }

    /**
     * Install the JAXRS security provider
     * @param providers - the JaxrsProviderBuildItem providers producer to use
     */
    @BuildStep
    void setupFilter(BuildProducer<JaxrsProviderBuildItem> providers) {
        providers.produce(new JaxrsProviderBuildItem(RolesFilterRegistrar.class.getName()));
    }

    private void registerReflectionForSerialization(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            CombinedIndexBuildItem combinedIndexBuildItem) {
        IndexView index = combinedIndexBuildItem.getIndex();

        // required by JSON-P support
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, "org.glassfish.json.JsonProviderImpl"));

        // required by Jackson
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false,
                "com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector",
                "com.fasterxml.jackson.databind.ser.std.SqlDateSerializer"));

        // This is probably redundant with the automatic resolution we do just below but better be safe
        for (DotName i : Arrays.asList(XML_ROOT, JSONB_ANNOTATION)) {
            for (AnnotationInstance annotation : index.getAnnotations(i)) {
                if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                    reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, annotation.target().asClass().name().toString()));
                }
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
                for (Type param : method.parameters()) {
                    if (isReflectionDeclarationRequiredFor(param)) {
                        reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem(param));
                    }
                }
            }
        }

        // In the case of a constraint violation, these elements might be returned as entities and will be serialized
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, ViolationReport.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, ResteasyConstraintViolation.class.getName()));
    }

    private static void categorizeProviders(Set<String> availableProviders, MediaTypeMap<String> categorizedReaders,
                                            MediaTypeMap<String> categorizedWriters, MediaTypeMap<String> categorizedContextResolvers,
                                            Set<String> otherProviders) {
        for (String availableProvider : availableProviders) {
            try {
                Class<?> providerClass = Class.forName(availableProvider);
                if (MessageBodyReader.class.isAssignableFrom(providerClass) || MessageBodyWriter.class.isAssignableFrom(providerClass)) {
                    if (MessageBodyReader.class.isAssignableFrom(providerClass)) {
                        Consumes consumes = providerClass.getAnnotation(Consumes.class);
                        if (consumes != null) {
                            for (String consumesMediaType : consumes.value()) {
                                categorizedReaders.add(MediaType.valueOf(consumesMediaType), providerClass.getName());
                            }
                        } else {
                            categorizedReaders.add(MediaType.WILDCARD_TYPE, providerClass.getName());
                        }
                    }
                    if (MessageBodyWriter.class.isAssignableFrom(providerClass)) {
                        Produces produces = providerClass.getAnnotation(Produces.class);
                        if (produces != null) {
                            for (String producesMediaType : produces.value()) {
                                categorizedWriters.add(MediaType.valueOf(producesMediaType), providerClass.getName());
                            }
                        } else {
                            categorizedWriters.add(MediaType.WILDCARD_TYPE, providerClass.getName());
                        }
                    }
                } else if (ContextResolver.class.isAssignableFrom(providerClass)) {
                    Produces produces = providerClass.getAnnotation(Produces.class);
                    if (produces != null) {
                        for (String producesMediaType : produces.value()) {
                            categorizedContextResolvers.add(MediaType.valueOf(producesMediaType),
                                    providerClass.getName());
                        }
                    } else {
                        categorizedContextResolvers.add(MediaType.WILDCARD_TYPE, providerClass.getName());
                    }
                } else {
                    otherProviders.add(providerClass.getName());
                }
            } catch (ClassNotFoundException e) {
                // Ignore
            }
        }
    }

    private static boolean collectDeclaredProviders(Set<String> providersToRegister,
                                                    MediaTypeMap<String> categorizedReaders, MediaTypeMap<String> categorizedWriters,
                                                    MediaTypeMap<String> categorizedContextResolvers, IndexView index) {
        for (ProviderDiscoverer providerDiscoverer : PROVIDER_DISCOVERERS) {
            Collection<AnnotationInstance> getMethods = index.getAnnotations(providerDiscoverer.getMethodAnnotation());
            for (AnnotationInstance getMethod : getMethods) {
                MethodInfo methodTarget = getMethod.target().asMethod();
                if (collectDeclaredProvidersForMethodAndMediaTypeAnnotation(providersToRegister, categorizedReaders,
                        methodTarget, CONSUMES, providerDiscoverer.noConsumesDefaultsToAll())) {
                    return true;
                }
                if (collectDeclaredProvidersForMethodAndMediaTypeAnnotation(providersToRegister, categorizedWriters,
                        methodTarget, PRODUCES, providerDiscoverer.noProducesDefaultsToAll())) {
                    return true;
                }
                if (collectDeclaredProvidersForMethodAndMediaTypeAnnotation(providersToRegister,
                        categorizedContextResolvers, methodTarget, PRODUCES,
                        providerDiscoverer.noProducesDefaultsToAll())) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean collectDeclaredProvidersForMethodAndMediaTypeAnnotation(Set<String> providersToRegister,
                                                                                   MediaTypeMap<String> categorizedProviders, MethodInfo methodTarget, DotName mediaTypeAnnotation,
                                                                                   boolean defaultsToAll) {
        AnnotationInstance mediaTypeAnnotationInstance = methodTarget.annotation(mediaTypeAnnotation);
        if (mediaTypeAnnotationInstance == null) {
            // let's consider the class
            Collection<AnnotationInstance> classAnnotations = methodTarget.declaringClass().classAnnotations();
            for (AnnotationInstance classAnnotation : classAnnotations) {
                if (mediaTypeAnnotation.equals(classAnnotation.name())) {
                    if (collectDeclaredProvidersForMediaTypeAnnotationInstance(providersToRegister, categorizedProviders,
                            classAnnotation)) {
                        return true;
                    }
                    return false;
                }
            }
            return defaultsToAll;
        }
        if (collectDeclaredProvidersForMediaTypeAnnotationInstance(providersToRegister, categorizedProviders,
                mediaTypeAnnotationInstance)) {
            return true;
        }

        return false;
    }

    private static boolean collectDeclaredProvidersForMediaTypeAnnotationInstance(Set<String> providersToRegister,
                                                                                  MediaTypeMap<String> categorizedProviders, AnnotationInstance mediaTypeAnnotationInstance) {
        for (String media : mediaTypeAnnotationInstance.value().asStringArray()) {
            MediaType mediaType = MediaType.valueOf(media);
            if (MediaType.WILDCARD_TYPE.equals(mediaType)) {
                // exit early if we have the wildcard type
                return true;
            }
            providersToRegister.addAll(categorizedProviders.getPossible(mediaType));
        }
        return false;
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

    private static class ProviderDiscoverer {

        private final DotName methodAnnotation;

        private final boolean noConsumesDefaultsToAll;

        private final boolean noProducesDefaultsToAll;

        private ProviderDiscoverer(DotName methodAnnotation, boolean noConsumesDefaultsToAll,
                                   boolean noProducesDefaultsToAll) {
            this.methodAnnotation = methodAnnotation;
            this.noConsumesDefaultsToAll = noConsumesDefaultsToAll;
            this.noProducesDefaultsToAll = noProducesDefaultsToAll;
        }

        public DotName getMethodAnnotation() {
            return methodAnnotation;
        }

        public boolean noConsumesDefaultsToAll() {
            return noConsumesDefaultsToAll;
        }

        public boolean noProducesDefaultsToAll() {
            return noProducesDefaultsToAll;
        }
    }
}
