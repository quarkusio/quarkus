package io.quarkus.undertow.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static io.undertow.servlet.api.SecurityInfo.EmptyRoleSemantic.DENY;
import static io.undertow.servlet.api.SecurityInfo.EmptyRoleSemantic.PERMIT;
import static javax.servlet.DispatcherType.REQUEST;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.annotation.HandlesTypes;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;

import org.jboss.annotation.javaee.Descriptions;
import org.jboss.annotation.javaee.DisplayNames;
import org.jboss.annotation.javaee.Icons;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.jboss.metadata.javaee.spec.DescriptionGroupMetaData;
import org.jboss.metadata.javaee.spec.DescriptionImpl;
import org.jboss.metadata.javaee.spec.DescriptionsImpl;
import org.jboss.metadata.javaee.spec.DisplayNameImpl;
import org.jboss.metadata.javaee.spec.DisplayNamesImpl;
import org.jboss.metadata.javaee.spec.IconImpl;
import org.jboss.metadata.javaee.spec.IconsImpl;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.javaee.spec.RunAsMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleRefMetaData;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.metadata.web.spec.AnnotationMetaData;
import org.jboss.metadata.web.spec.AnnotationsMetaData;
import org.jboss.metadata.web.spec.DispatcherType;
import org.jboss.metadata.web.spec.EmptyRoleSemanticType;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.metadata.web.spec.HttpMethodConstraintMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.MultipartConfigMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.metadata.web.spec.ServletSecurityMetaData;
import org.jboss.metadata.web.spec.ServletsMetaData;
import org.jboss.metadata.web.spec.TransportGuaranteeType;
import org.jboss.metadata.web.spec.WebMetaData;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.ContextRegistrarBuildItem;
import io.quarkus.arc.deployment.RuntimeBeanBuildItem;
import io.quarkus.arc.processor.ContextRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ObjectSubstitutionBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateConfigBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.undertow.runtime.HttpBuildConfig;
import io.quarkus.undertow.runtime.HttpConfig;
import io.quarkus.undertow.runtime.HttpSessionContext;
import io.quarkus.undertow.runtime.ServletProducer;
import io.quarkus.undertow.runtime.ServletSecurityInfoProxy;
import io.quarkus.undertow.runtime.ServletSecurityInfoSubstitution;
import io.quarkus.undertow.runtime.UndertowDeploymentRecorder;
import io.quarkus.undertow.runtime.UndertowHandlersConfServletExtension;
import io.quarkus.undertow.runtime.filters.CORSRecorder;
import io.undertow.Undertow;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.HttpMethodSecurityInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.api.ServletSecurityInfo;
import io.undertow.servlet.handlers.DefaultServlet;

//TODO: break this up, it is getting too big
public class UndertowBuildStep {

    public static final DotName WEB_FILTER = DotName.createSimple(WebFilter.class.getName());
    public static final DotName WEB_LISTENER = DotName.createSimple(WebListener.class.getName());
    public static final DotName WEB_SERVLET = DotName.createSimple(WebServlet.class.getName());
    public static final DotName RUN_AS = DotName.createSimple(RunAs.class.getName());
    public static final DotName DECLARE_ROLES = DotName.createSimple(DeclareRoles.class.getName());
    public static final DotName MULTIPART_CONFIG = DotName.createSimple(MultipartConfig.class.getName());
    public static final DotName SERVLET_SECURITY = DotName.createSimple(ServletSecurity.class.getName());
    protected static final String SERVLET_CONTAINER_INITIALIZER = "META-INF/services/javax.servlet.ServletContainerInitializer";
    protected static final DotName HANDLES_TYPES = DotName.createSimple(HandlesTypes.class.getName());

    @Inject
    CombinedIndexBuildItem combinedIndexBuildItem;

    @BuildStep
    @Record(RUNTIME_INIT)
    public ServiceStartBuildItem boot(UndertowDeploymentRecorder recorder,
            ServletDeploymentManagerBuildItem servletDeploymentManagerBuildItem,
            List<HttpHandlerWrapperBuildItem> wrappers,
            ShutdownContextBuildItem shutdown,
            Consumer<UndertowBuildItem> undertowProducer,
            LaunchModeBuildItem launchMode,
            ExecutorBuildItem executorBuildItem,
            CORSRecorder corsRecorder,
            HttpConfig config) throws Exception {
        corsRecorder.setHttpConfig(config);
        RuntimeValue<Undertow> ut = recorder.startUndertow(shutdown, executorBuildItem.getExecutorProxy(),
                servletDeploymentManagerBuildItem.getDeploymentManager(),
                config, wrappers.stream().map(HttpHandlerWrapperBuildItem::getValue).collect(Collectors.toList()),
                launchMode.getLaunchMode());
        undertowProducer.accept(new UndertowBuildItem(ut));
        return new ServiceStartBuildItem("undertow");
    }

    @BuildStep()
    @Record(STATIC_INIT)
    public void buildCorsFilter(CORSRecorder corsRecorder, HttpBuildConfig buildConfig,
            BuildProducer<ServletExtensionBuildItem> extensionProducer) {
        if (buildConfig.corsEnabled) {
            extensionProducer.produce(new ServletExtensionBuildItem(corsRecorder.buildCORSExtension()));
        }
    }

    @BuildStep
    void integrateCdi(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ContextRegistrarBuildItem> contextRegistrars,
            BuildProducer<ListenerBuildItem> listeners) {
        additionalBeans.produce(new AdditionalBeanBuildItem(ServletProducer.class));
        contextRegistrars.produce(new ContextRegistrarBuildItem(new ContextRegistrar() {
            @Override
            public void register(RegistrationContext registrationContext) {
                registrationContext.configure(SessionScoped.class).normal().contextClass(HttpSessionContext.class).done();
            }
        }));
        listeners.produce(new ListenerBuildItem(HttpSessionContext.class.getName()));
    }

    @BuildStep
    SubstrateConfigBuildItem config() {
        return SubstrateConfigBuildItem.builder()
                .addRuntimeInitializedClass("io.undertow.server.protocol.ajp.AjpServerResponseConduit")
                .addRuntimeInitializedClass("io.undertow.server.protocol.ajp.AjpServerRequestConduit")
                .build();
    }

    @BuildStep
    void runtimeReinit(BuildProducer<RuntimeReinitializedClassBuildItem> producer) {
        producer.produce(new RuntimeReinitializedClassBuildItem("org.wildfly.common.net.HostName"));
        producer.produce(new RuntimeReinitializedClassBuildItem("org.wildfly.common.os.Process"));
    }

    @BuildStep
    public void kubernetes(HttpConfig config, BuildProducer<KubernetesPortBuildItem> portProducer) {
        portProducer.produce(new KubernetesPortBuildItem(config.port, "http"));
    }

    /**
     * Register the undertow-handlers.conf file
     */
    @BuildStep
    @Record(STATIC_INIT)
    public void registerUndertowHandlersConf(BuildProducer<ServletExtensionBuildItem> producer,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFile,
            BuildProducer<SubstrateResourceBuildItem> substrateResourceBuildItemBuildProducer) {
        //we always watch the file, so if it gets added we restart
        watchedFile.produce(
                new HotDeploymentWatchedFileBuildItem(UndertowHandlersConfServletExtension.META_INF_UNDERTOW_HANDLERS_CONF));

        //check for the file in the handlers dir
        Path handlerPath = applicationArchivesBuildItem.getRootArchive()
                .getChildPath(UndertowHandlersConfServletExtension.META_INF_UNDERTOW_HANDLERS_CONF);
        if (handlerPath != null) {
            producer.produce(new ServletExtensionBuildItem(new UndertowHandlersConfServletExtension()));
            substrateResourceBuildItemBuildProducer.produce(
                    new SubstrateResourceBuildItem(UndertowHandlersConfServletExtension.META_INF_UNDERTOW_HANDLERS_CONF));
        }
    }

    /*
     * look for Servlet container initializers
     *
     */
    @BuildStep
    public List<ServletContainerInitializerBuildItem> servletContainerInitializer(
            ApplicationArchivesBuildItem archives,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<AdditionalBeanBuildItem> beans) throws IOException {
        List<ServletContainerInitializerBuildItem> ret = new ArrayList<>();
        Set<String> initializers = ServiceUtil.classNamesNamedIn(Thread.currentThread().getContextClassLoader(),
                SERVLET_CONTAINER_INITIALIZER);
        for (String initializer : initializers) {
            beans.produce(AdditionalBeanBuildItem.unremovableOf(initializer));
            ClassInfo sci = combinedIndexBuildItem.getIndex().getClassByName(DotName.createSimple(initializer));
            if (sci != null) {
                AnnotationInstance handles = sci.classAnnotation(HANDLES_TYPES);
                Set<String> handledTypes = new HashSet<>();
                if (handles != null) {
                    Type[] types = handles.value().asClassArray();
                    for (Type handledType : types) {
                        DotName typeName = handledType.asClassType().name();
                        for (ClassInfo classInfo : combinedIndexBuildItem.getIndex().getAllKnownSubclasses(typeName)) {
                            handledTypes.add(classInfo.name().toString());
                        }
                        for (ClassInfo classInfo : combinedIndexBuildItem.getIndex().getAllKnownImplementors(typeName)) {
                            handledTypes.add(classInfo.name().toString());
                        }
                    }
                }
                ret.add(new ServletContainerInitializerBuildItem(initializer, handledTypes));
            } else {
                ret.add(new ServletContainerInitializerBuildItem(initializer, Collections.emptySet()));
            }
        }
        return ret;
    }

    @Record(STATIC_INIT)
    @BuildStep()
    public ServletDeploymentManagerBuildItem build(List<ServletBuildItem> servlets,
            List<FilterBuildItem> filters,
            List<ListenerBuildItem> listeners,
            List<ServletInitParamBuildItem> initParams,
            List<ServletContextAttributeBuildItem> contextParams,
            List<ServletContainerInitializerBuildItem> servletContainerInitializerBuildItems,
            UndertowDeploymentRecorder recorder, RecorderContext context,
            List<ServletExtensionBuildItem> extensions,
            BeanContainerBuildItem bc,
            WebMetadataBuildItem webMetadataBuildItem,
            BuildProducer<ObjectSubstitutionBuildItem> substitutions,
            Consumer<ReflectiveClassBuildItem> reflectiveClasses,
            LaunchModeBuildItem launchMode,
            ShutdownContextBuildItem shutdownContext,
            KnownPathsBuildItem knownPaths) throws Exception {

        ObjectSubstitutionBuildItem.Holder holder = new ObjectSubstitutionBuildItem.Holder(ServletSecurityInfo.class,
                ServletSecurityInfoProxy.class, ServletSecurityInfoSubstitution.class);
        substitutions.produce(new ObjectSubstitutionBuildItem(holder));
        reflectiveClasses.accept(new ReflectiveClassBuildItem(false, false, DefaultServlet.class.getName(),
                "io.undertow.server.protocol.http.HttpRequestParser$$generated"));

        RuntimeValue<DeploymentInfo> deployment = recorder.createDeployment("test", knownPaths.knownFiles,
                knownPaths.knownDirectories,
                launchMode.getLaunchMode(), shutdownContext);

        WebMetaData webMetaData = webMetadataBuildItem.getWebMetaData();
        final IndexView index = combinedIndexBuildItem.getIndex();
        processAnnotations(index, webMetaData);
        //add servlets
        if (webMetaData.getServlets() != null) {
            for (ServletMetaData servlet : webMetaData.getServlets()) {
                reflectiveClasses.accept(new ReflectiveClassBuildItem(false, false, servlet.getServletClass()));
                Map<String, String> params = new HashMap<>();
                for (Map.Entry<String, String> i : params.entrySet()) {
                    params.put(i.getKey(), i.getValue());
                }
                RuntimeValue<ServletInfo> sref = recorder.registerServlet(deployment, servlet.getServletName(),
                        context.classProxy(servlet.getServletClass()),
                        servlet.isAsyncSupported(),
                        servlet.getLoadOnStartupInt(),
                        bc.getValue(),
                        params,
                        null);
                if (servlet.getInitParam() != null) {
                    for (ParamValueMetaData init : servlet.getInitParam()) {
                        recorder.addServletInitParam(sref, init.getParamName(), init.getParamValue());
                    }
                }
                if (servlet.getMultipartConfig() != null) {
                    recorder.setMultipartConfig(sref, servlet.getMultipartConfig().getLocation(),
                            servlet.getMultipartConfig().getMaxFileSize(), servlet.getMultipartConfig().getMaxRequestSize(),
                            servlet.getMultipartConfig().getFileSizeThreshold());
                }
                // Map the @ServletSecurity annotations
                if (webMetaData.getAnnotations() != null) {
                    for (AnnotationMetaData amd : webMetaData.getAnnotations()) {
                        if (amd.getClassName().equals(servlet.getServletClass())) {
                            // Process the @ServletSecurity into metadata
                            ServletSecurityMetaData ssmd = amd.getServletSecurity();
                            ServletSecurityInfo securityInfo = new ServletSecurityInfo();
                            securityInfo.setEmptyRoleSemantic(
                                    ssmd.getEmptyRoleSemantic() == EmptyRoleSemanticType.DENY ? DENY : PERMIT);
                            securityInfo.setTransportGuaranteeType(transportGuaranteeType(ssmd.getTransportGuarantee()))
                                    .addRolesAllowed(ssmd.getRolesAllowed());
                            if (ssmd.getHttpMethodConstraints() != null) {
                                for (HttpMethodConstraintMetaData method : ssmd.getHttpMethodConstraints()) {
                                    securityInfo.addHttpMethodSecurityInfo(
                                            new HttpMethodSecurityInfo()
                                                    .setEmptyRoleSemantic(
                                                            method.getEmptyRoleSemantic() == EmptyRoleSemanticType.DENY ? DENY
                                                                    : PERMIT)
                                                    .setTransportGuaranteeType(
                                                            transportGuaranteeType(method.getTransportGuarantee()))
                                                    .addRolesAllowed(method.getRolesAllowed())
                                                    .setMethod(method.getMethod()));
                                }
                            }
                            recorder.setSecurityInfo(sref, securityInfo);
                        }
                        if (servlet.getSecurityRoleRefs() != null) {
                            for (final SecurityRoleRefMetaData ref : servlet.getSecurityRoleRefs()) {
                                recorder.addSecurityRoleRef(sref, ref.getRoleName(), ref.getRoleLink());
                            }
                        }
                    }
                }
            }
        }
        //servlet mappings
        if (webMetaData.getServletMappings() != null) {
            for (ServletMappingMetaData mapping : webMetaData.getServletMappings()) {
                for (String m : mapping.getUrlPatterns()) {
                    recorder.addServletMapping(deployment, mapping.getServletName(), m);
                }
            }
        }
        //filters
        if (webMetaData.getFilters() != null) {
            for (FilterMetaData filter : webMetaData.getFilters()) {
                reflectiveClasses.accept(new ReflectiveClassBuildItem(false, false, filter.getFilterClass()));
                Map<String, String> params = new HashMap<>();
                for (Map.Entry<String, String> i : params.entrySet()) {
                    params.put(i.getKey(), i.getValue());
                }
                RuntimeValue<FilterInfo> sref = recorder.registerFilter(deployment,
                        filter.getFilterName(),
                        context.classProxy(filter.getFilterClass()),
                        filter.isAsyncSupported(),
                        bc.getValue(),
                        params, null);
                if (filter.getInitParam() != null) {
                    for (ParamValueMetaData init : filter.getInitParam()) {
                        recorder.addFilterInitParam(sref, init.getParamName(), init.getParamValue());
                    }
                }
            }
        }
        if (webMetaData.getFilterMappings() != null) {
            for (FilterMappingMetaData mapping : webMetaData.getFilterMappings()) {
                for (String m : mapping.getUrlPatterns()) {
                    if (mapping.getDispatchers() == null || mapping.getDispatchers().isEmpty()) {
                        recorder.addFilterURLMapping(deployment, mapping.getFilterName(), m, REQUEST);
                    } else {

                        for (DispatcherType dispatcher : mapping.getDispatchers()) {
                            recorder.addFilterURLMapping(deployment, mapping.getFilterName(), m,
                                    javax.servlet.DispatcherType.valueOf(dispatcher.name()));
                        }
                    }
                }
            }
        }

        //listeners
        if (webMetaData.getListeners() != null) {
            for (ListenerMetaData listener : webMetaData.getListeners()) {
                reflectiveClasses.accept(new ReflectiveClassBuildItem(false, false, listener.getListenerClass()));
                recorder.registerListener(deployment, context.classProxy(listener.getListenerClass()), bc.getValue());
            }
        }

        for (ServletBuildItem servlet : servlets) {
            String servletClass = servlet.getServletClass();
            if (servlet.getLoadOnStartup() == 0) {
                reflectiveClasses.accept(new ReflectiveClassBuildItem(false, false, servlet.getServletClass()));
            }
            recorder.registerServlet(deployment, servlet.getName(), context.classProxy(servletClass),
                    servlet.isAsyncSupported(), servlet.getLoadOnStartup(), bc.getValue(), servlet.getInitParams(),
                    servlet.getInstanceFactory());

            for (String m : servlet.getMappings()) {
                recorder.addServletMapping(deployment, servlet.getName(), m);
            }
        }

        for (FilterBuildItem filter : filters) {
            String filterClass = filter.getFilterClass();
            reflectiveClasses.accept(new ReflectiveClassBuildItem(false, false, filterClass));
            recorder.registerFilter(deployment, filter.getName(), context.classProxy(filterClass), filter.isAsyncSupported(),
                    bc.getValue(), filter.getInitParams(), filter.getInstanceFactory());
            for (FilterBuildItem.FilterMappingInfo m : filter.getMappings()) {
                if (m.getMappingType() == FilterBuildItem.FilterMappingInfo.MappingType.URL) {
                    recorder.addFilterURLMapping(deployment, filter.getName(), m.getMapping(), m.getDispatcher());
                } else {
                    recorder.addFilterServletNameMapping(deployment, filter.getName(), m.getMapping(), m.getDispatcher());
                }
            }
        }
        for (ServletInitParamBuildItem i : initParams) {
            recorder.addServletInitParameter(deployment, i.getKey(), i.getValue());
        }
        for (ServletContextAttributeBuildItem i : contextParams) {
            recorder.addServletContextAttribute(deployment, i.getKey(), i.getValue());
        }
        for (ServletExtensionBuildItem i : extensions) {
            recorder.addServletExtension(deployment, i.getValue());
        }
        for (ListenerBuildItem i : listeners) {
            reflectiveClasses.accept(new ReflectiveClassBuildItem(false, false, i.getListenerClass()));
            recorder.registerListener(deployment, context.classProxy(i.getListenerClass()), bc.getValue());
        }

        for (ServletContainerInitializerBuildItem sci : servletContainerInitializerBuildItems) {
            Set<Class<?>> handlesTypes = new HashSet<>();
            for (String handledType : sci.handlesTypes) {
                handlesTypes.add(context.classProxy(handledType));
            }

            recorder.addServletContainerInitializer(deployment,
                    (Class<? extends ServletContainerInitializer>) context.classProxy(sci.sciClass), handlesTypes);
        }

        return new ServletDeploymentManagerBuildItem(
                recorder.bootServletContainer(deployment, bc.getValue(), launchMode.getLaunchMode(), shutdownContext));

    }

    @BuildStep
    @Record(STATIC_INIT)
    RuntimeBeanBuildItem servletContextBean(
            UndertowDeploymentRecorder recorder) {
        return RuntimeBeanBuildItem.builder(ServletContext.class).setScope(ApplicationScoped.class)
                .setSupplier((Supplier) recorder.servletContextSupplier())
                .build();
    }

    /**
     * Process a single index.
     *
     * @param index the annotation index
     */
    private void processAnnotations(IndexView index, WebMetaData metaData) {
        // @WebServlet
        final Collection<AnnotationInstance> webServletAnnotations = index.getAnnotations(WEB_SERVLET);
        if (webServletAnnotations != null && webServletAnnotations.size() > 0) {
            ServletsMetaData servlets = metaData.getServlets();
            if (servlets == null) {
                servlets = new ServletsMetaData();
                metaData.setServlets(servlets);
            }
            List<ServletMappingMetaData> servletMappings = metaData.getServletMappings();
            if (servletMappings == null) {
                servletMappings = new ArrayList<>();
                metaData.setServletMappings(servletMappings);
            }
            for (final AnnotationInstance annotation : webServletAnnotations) {
                ServletMetaData servlet = new ServletMetaData();
                AnnotationTarget target = annotation.target();
                ClassInfo classInfo = ClassInfo.class.cast(target);
                servlet.setServletClass(classInfo.toString());
                AnnotationValue nameValue = annotation.value("name");
                if (nameValue == null || nameValue.asString().isEmpty()) {
                    servlet.setName(classInfo.toString());
                } else {
                    servlet.setName(nameValue.asString());
                }
                AnnotationValue loadOnStartup = annotation.value("loadOnStartup");
                if (loadOnStartup != null && loadOnStartup.asInt() >= 0) {
                    servlet.setLoadOnStartupInt(loadOnStartup.asInt());
                }
                AnnotationValue asyncSupported = annotation.value("asyncSupported");
                if (asyncSupported != null) {
                    servlet.setAsyncSupported(asyncSupported.asBoolean());
                }
                AnnotationValue initParamsValue = annotation.value("initParams");
                if (initParamsValue != null) {
                    AnnotationInstance[] initParamsAnnotations = initParamsValue.asNestedArray();
                    if (initParamsAnnotations != null && initParamsAnnotations.length > 0) {
                        List<ParamValueMetaData> initParams = new ArrayList<ParamValueMetaData>();
                        for (AnnotationInstance initParamsAnnotation : initParamsAnnotations) {
                            ParamValueMetaData initParam = new ParamValueMetaData();
                            AnnotationValue initParamName = initParamsAnnotation.value("name");
                            AnnotationValue initParamValue = initParamsAnnotation.value();
                            AnnotationValue initParamDescription = initParamsAnnotation.value("description");
                            initParam.setParamName(initParamName.asString());
                            initParam.setParamValue(initParamValue.asString());
                            if (initParamDescription != null) {
                                Descriptions descriptions = getDescription(initParamDescription.asString());
                                if (descriptions != null) {
                                    initParam.setDescriptions(descriptions);
                                }
                            }
                            initParams.add(initParam);
                        }
                        servlet.setInitParam(initParams);
                    }
                }
                AnnotationValue descriptionValue = annotation.value("description");
                AnnotationValue displayNameValue = annotation.value("displayName");
                AnnotationValue smallIconValue = annotation.value("smallIcon");
                AnnotationValue largeIconValue = annotation.value("largeIcon");
                DescriptionGroupMetaData descriptionGroup = getDescriptionGroup(
                        (descriptionValue == null) ? "" : descriptionValue.asString(),
                        (displayNameValue == null) ? "" : displayNameValue.asString(),
                        (smallIconValue == null) ? "" : smallIconValue.asString(),
                        (largeIconValue == null) ? "" : largeIconValue.asString());
                if (descriptionGroup != null) {
                    servlet.setDescriptionGroup(descriptionGroup);
                }
                ServletMappingMetaData servletMapping = new ServletMappingMetaData();
                servletMapping.setServletName(servlet.getName());
                List<String> urlPatterns = new ArrayList<String>();
                AnnotationValue urlPatternsValue = annotation.value("urlPatterns");
                if (urlPatternsValue != null) {
                    for (String urlPattern : urlPatternsValue.asStringArray()) {
                        urlPatterns.add(urlPattern);
                    }
                }
                urlPatternsValue = annotation.value();
                if (urlPatternsValue != null) {
                    for (String urlPattern : urlPatternsValue.asStringArray()) {
                        urlPatterns.add(urlPattern);
                    }
                }
                if (urlPatterns.size() > 0) {
                    servletMapping.setUrlPatterns(urlPatterns);
                    servletMappings.add(servletMapping);
                }
                servlets.add(servlet);
            }
        }
        // @WebFilter
        final Collection<AnnotationInstance> webFilterAnnotations = index.getAnnotations(WEB_FILTER);
        if (webFilterAnnotations != null && webFilterAnnotations.size() > 0) {
            FiltersMetaData filters = metaData.getFilters();
            if (filters == null) {
                filters = new FiltersMetaData();
                metaData.setFilters(filters);
            }
            List<FilterMappingMetaData> filterMappings = metaData.getFilterMappings();
            if (filterMappings == null) {
                filterMappings = new ArrayList<>();
                metaData.setFilterMappings(filterMappings);
            }
            for (final AnnotationInstance annotation : webFilterAnnotations) {
                FilterMetaData filter = new FilterMetaData();
                AnnotationTarget target = annotation.target();
                ClassInfo classInfo = ClassInfo.class.cast(target);
                filter.setFilterClass(classInfo.toString());
                AnnotationValue nameValue = annotation.value("filterName");
                if (nameValue == null || nameValue.asString().isEmpty()) {
                    filter.setName(classInfo.toString());
                } else {
                    filter.setName(nameValue.asString());
                }
                AnnotationValue asyncSupported = annotation.value("asyncSupported");
                if (asyncSupported != null) {
                    filter.setAsyncSupported(asyncSupported.asBoolean());
                }
                AnnotationValue initParamsValue = annotation.value("initParams");
                if (initParamsValue != null) {
                    AnnotationInstance[] initParamsAnnotations = initParamsValue.asNestedArray();
                    if (initParamsAnnotations != null && initParamsAnnotations.length > 0) {
                        List<ParamValueMetaData> initParams = new ArrayList<ParamValueMetaData>();
                        for (AnnotationInstance initParamsAnnotation : initParamsAnnotations) {
                            ParamValueMetaData initParam = new ParamValueMetaData();
                            AnnotationValue initParamName = initParamsAnnotation.value("name");
                            AnnotationValue initParamValue = initParamsAnnotation.value();
                            AnnotationValue initParamDescription = initParamsAnnotation.value("description");
                            initParam.setParamName(initParamName.asString());
                            initParam.setParamValue(initParamValue.asString());
                            if (initParamDescription != null) {
                                Descriptions descriptions = getDescription(initParamDescription.asString());
                                if (descriptions != null) {
                                    initParam.setDescriptions(descriptions);
                                }
                            }
                            initParams.add(initParam);
                        }
                        filter.setInitParam(initParams);
                    }
                }
                AnnotationValue descriptionValue = annotation.value("description");
                AnnotationValue displayNameValue = annotation.value("displayName");
                AnnotationValue smallIconValue = annotation.value("smallIcon");
                AnnotationValue largeIconValue = annotation.value("largeIcon");
                DescriptionGroupMetaData descriptionGroup = getDescriptionGroup(
                        (descriptionValue == null) ? "" : descriptionValue.asString(),
                        (displayNameValue == null) ? "" : displayNameValue.asString(),
                        (smallIconValue == null) ? "" : smallIconValue.asString(),
                        (largeIconValue == null) ? "" : largeIconValue.asString());
                if (descriptionGroup != null) {
                    filter.setDescriptionGroup(descriptionGroup);
                }
                filters.add(filter);
                FilterMappingMetaData filterMapping = new FilterMappingMetaData();
                filterMapping.setFilterName(filter.getName());
                List<String> urlPatterns = new ArrayList<String>();
                List<String> servletNames = new ArrayList<String>();
                List<DispatcherType> dispatchers = new ArrayList<DispatcherType>();
                AnnotationValue urlPatternsValue = annotation.value("urlPatterns");
                if (urlPatternsValue != null) {
                    for (String urlPattern : urlPatternsValue.asStringArray()) {
                        urlPatterns.add(urlPattern);
                    }
                }
                urlPatternsValue = annotation.value();
                if (urlPatternsValue != null) {
                    for (String urlPattern : urlPatternsValue.asStringArray()) {
                        urlPatterns.add(urlPattern);
                    }
                }
                if (urlPatterns.size() > 0) {
                    filterMapping.setUrlPatterns(urlPatterns);
                }
                AnnotationValue servletNamesValue = annotation.value("servletNames");
                if (servletNamesValue != null) {
                    for (String servletName : servletNamesValue.asStringArray()) {
                        servletNames.add(servletName);
                    }
                }
                if (servletNames.size() > 0) {
                    filterMapping.setServletNames(servletNames);
                }
                AnnotationValue dispatcherTypesValue = annotation.value("dispatcherTypes");
                if (dispatcherTypesValue != null) {
                    for (String dispatcherValue : dispatcherTypesValue.asEnumArray()) {
                        dispatchers.add(DispatcherType.valueOf(dispatcherValue));
                    }
                }
                if (dispatchers.size() > 0) {
                    filterMapping.setDispatchers(dispatchers);
                }
                if (urlPatterns.size() > 0 || servletNames.size() > 0) {
                    filterMappings.add(filterMapping);
                }
            }
        }
        // @WebListener
        final Collection<AnnotationInstance> webListenerAnnotations = index.getAnnotations(WEB_LISTENER);
        if (webListenerAnnotations != null && webListenerAnnotations.size() > 0) {
            List<ListenerMetaData> listeners = metaData.getListeners();
            if (listeners == null) {
                listeners = new ArrayList<>();
                metaData.setListeners(listeners);
            }
            for (final AnnotationInstance annotation : webListenerAnnotations) {
                ListenerMetaData listener = new ListenerMetaData();
                AnnotationTarget target = annotation.target();
                ClassInfo classInfo = ClassInfo.class.cast(target);
                listener.setListenerClass(classInfo.toString());
                AnnotationValue descriptionValue = annotation.value();
                if (descriptionValue != null) {
                    DescriptionGroupMetaData descriptionGroup = getDescriptionGroup(descriptionValue.asString());
                    if (descriptionGroup != null) {
                        listener.setDescriptionGroup(descriptionGroup);
                    }
                }
                listeners.add(listener);
            }
        }
        // @RunAs
        final Collection<AnnotationInstance> runAsAnnotations = index.getAnnotations(RUN_AS);
        if (runAsAnnotations != null && runAsAnnotations.size() > 0) {
            AnnotationsMetaData annotations = metaData.getAnnotations();
            if (annotations == null) {
                annotations = new AnnotationsMetaData();
                metaData.setAnnotations(annotations);
            }
            for (final AnnotationInstance annotation : runAsAnnotations) {
                AnnotationTarget target = annotation.target();
                if (!(target instanceof ClassInfo)) {
                    continue;
                }
                ClassInfo classInfo = ClassInfo.class.cast(target);
                AnnotationMetaData annotationMD = annotations.get(classInfo.toString());
                if (annotationMD == null) {
                    annotationMD = new AnnotationMetaData();
                    annotationMD.setClassName(classInfo.toString());
                    annotations.add(annotationMD);
                }
                RunAsMetaData runAs = new RunAsMetaData();
                runAs.setRoleName(annotation.value().asString());
                annotationMD.setRunAs(runAs);
            }
        }
        // @DeclareRoles
        final Collection<AnnotationInstance> declareRolesAnnotations = index.getAnnotations(DECLARE_ROLES);
        if (declareRolesAnnotations != null && declareRolesAnnotations.size() > 0) {
            SecurityRolesMetaData securityRoles = metaData.getSecurityRoles();
            if (securityRoles == null) {
                securityRoles = new SecurityRolesMetaData();
                metaData.setSecurityRoles(securityRoles);
            }
            for (final AnnotationInstance annotation : declareRolesAnnotations) {
                for (String role : annotation.value().asStringArray()) {
                    SecurityRoleMetaData sr = new SecurityRoleMetaData();
                    sr.setRoleName(role);
                    securityRoles.add(sr);
                }
            }
        }
        // @MultipartConfig
        final Collection<AnnotationInstance> multipartConfigAnnotations = index.getAnnotations(MULTIPART_CONFIG);
        if (multipartConfigAnnotations != null && multipartConfigAnnotations.size() > 0) {
            AnnotationsMetaData annotations = metaData.getAnnotations();
            if (annotations == null) {
                annotations = new AnnotationsMetaData();
                metaData.setAnnotations(annotations);
            }
            for (final AnnotationInstance annotation : multipartConfigAnnotations) {
                AnnotationTarget target = annotation.target();
                ClassInfo classInfo = ClassInfo.class.cast(target);
                AnnotationMetaData annotationMD = annotations.get(classInfo.toString());
                if (annotationMD == null) {
                    annotationMD = new AnnotationMetaData();
                    annotationMD.setClassName(classInfo.toString());
                    annotations.add(annotationMD);
                }
                MultipartConfigMetaData multipartConfig = new MultipartConfigMetaData();
                AnnotationValue locationValue = annotation.value("location");
                if (locationValue != null && locationValue.asString().length() > 0) {
                    multipartConfig.setLocation(locationValue.asString());
                }
                AnnotationValue maxFileSizeValue = annotation.value("maxFileSize");
                if (maxFileSizeValue != null && maxFileSizeValue.asLong() != -1L) {
                    multipartConfig.setMaxFileSize(maxFileSizeValue.asLong());
                }
                AnnotationValue maxRequestSizeValue = annotation.value("maxRequestSize");
                if (maxRequestSizeValue != null && maxRequestSizeValue.asLong() != -1L) {
                    multipartConfig.setMaxRequestSize(maxRequestSizeValue.asLong());
                }
                AnnotationValue fileSizeThresholdValue = annotation.value("fileSizeThreshold");
                if (fileSizeThresholdValue != null && fileSizeThresholdValue.asInt() != 0) {
                    multipartConfig.setFileSizeThreshold(fileSizeThresholdValue.asInt());
                }
                annotationMD.setMultipartConfig(multipartConfig);
            }
        }
        // @ServletSecurity
        final Collection<AnnotationInstance> servletSecurityAnnotations = index.getAnnotations(SERVLET_SECURITY);
        if (servletSecurityAnnotations != null && servletSecurityAnnotations.size() > 0) {
            AnnotationsMetaData annotations = metaData.getAnnotations();
            if (annotations == null) {
                annotations = new AnnotationsMetaData();
                metaData.setAnnotations(annotations);
            }
            for (final AnnotationInstance annotation : servletSecurityAnnotations) {
                AnnotationTarget target = annotation.target();
                ClassInfo classInfo = ClassInfo.class.cast(target);
                AnnotationMetaData annotationMD = annotations.get(classInfo.toString());
                if (annotationMD == null) {
                    annotationMD = new AnnotationMetaData();
                    annotationMD.setClassName(classInfo.toString());
                    annotations.add(annotationMD);
                }
                ServletSecurityMetaData servletSecurity = new ServletSecurityMetaData();
                AnnotationValue httpConstraintValue = annotation.value();
                List<String> rolesAllowed = new ArrayList<String>();
                if (httpConstraintValue != null) {
                    AnnotationInstance httpConstraint = httpConstraintValue.asNested();
                    AnnotationValue httpConstraintERSValue = httpConstraint.value();
                    if (httpConstraintERSValue != null) {
                        servletSecurity.setEmptyRoleSemantic(EmptyRoleSemanticType.valueOf(httpConstraintERSValue.asEnum()));
                    }
                    AnnotationValue httpConstraintTGValue = httpConstraint.value("transportGuarantee");
                    if (httpConstraintTGValue != null) {
                        servletSecurity.setTransportGuarantee(TransportGuaranteeType.valueOf(httpConstraintTGValue.asEnum()));
                    }
                    AnnotationValue rolesAllowedValue = httpConstraint.value("rolesAllowed");
                    if (rolesAllowedValue != null) {
                        for (String role : rolesAllowedValue.asStringArray()) {
                            rolesAllowed.add(role);
                        }
                    }
                }
                servletSecurity.setRolesAllowed(rolesAllowed);
                AnnotationValue httpMethodConstraintsValue = annotation.value("httpMethodConstraints");
                if (httpMethodConstraintsValue != null) {
                    AnnotationInstance[] httpMethodConstraints = httpMethodConstraintsValue.asNestedArray();
                    if (httpMethodConstraints.length > 0) {
                        List<HttpMethodConstraintMetaData> methodConstraints = new ArrayList<HttpMethodConstraintMetaData>();
                        for (AnnotationInstance httpMethodConstraint : httpMethodConstraints) {
                            HttpMethodConstraintMetaData methodConstraint = new HttpMethodConstraintMetaData();
                            AnnotationValue httpMethodConstraintValue = httpMethodConstraint.value();
                            if (httpMethodConstraintValue != null) {
                                methodConstraint.setMethod(httpMethodConstraintValue.asString());
                            }
                            AnnotationValue httpMethodConstraintERSValue = httpMethodConstraint.value("emptyRoleSemantic");
                            if (httpMethodConstraintERSValue != null) {
                                methodConstraint.setEmptyRoleSemantic(
                                        EmptyRoleSemanticType.valueOf(httpMethodConstraintERSValue.asEnum()));
                            }
                            AnnotationValue httpMethodConstraintTGValue = httpMethodConstraint.value("transportGuarantee");
                            if (httpMethodConstraintTGValue != null) {
                                methodConstraint.setTransportGuarantee(
                                        TransportGuaranteeType.valueOf(httpMethodConstraintTGValue.asEnum()));
                            }
                            AnnotationValue rolesAllowedValue = httpMethodConstraint.value("rolesAllowed");
                            rolesAllowed = new ArrayList<String>();
                            if (rolesAllowedValue != null) {
                                for (String role : rolesAllowedValue.asStringArray()) {
                                    rolesAllowed.add(role);
                                }
                            }
                            methodConstraint.setRolesAllowed(rolesAllowed);
                            methodConstraints.add(methodConstraint);
                        }
                        servletSecurity.setHttpMethodConstraints(methodConstraints);
                    }
                }
                annotationMD.setServletSecurity(servletSecurity);
            }
        }
    }

    /**
     * Map web metadata type to undertow type
     *
     * @param type web metadata TransportGuaranteeType
     * @return undertow TransportGuaranteeType
     */
    private static io.undertow.servlet.api.TransportGuaranteeType transportGuaranteeType(final TransportGuaranteeType type) {
        if (type == null) {
            return io.undertow.servlet.api.TransportGuaranteeType.NONE;
        }
        switch (type) {
            case CONFIDENTIAL:
                return io.undertow.servlet.api.TransportGuaranteeType.CONFIDENTIAL;
            case INTEGRAL:
                return io.undertow.servlet.api.TransportGuaranteeType.INTEGRAL;
            case NONE:
                return io.undertow.servlet.api.TransportGuaranteeType.NONE;
        }
        throw new RuntimeException("UNREACHABLE");
    }

    protected Descriptions getDescription(String description) {
        DescriptionsImpl descriptions = null;
        if (description.length() > 0) {
            DescriptionImpl di = new DescriptionImpl();
            di.setDescription(description);
            descriptions = new DescriptionsImpl();
            descriptions.add(di);
        }
        return descriptions;
    }

    protected DisplayNames getDisplayName(String displayName) {
        DisplayNamesImpl displayNames = null;
        if (displayName.length() > 0) {
            DisplayNameImpl dn = new DisplayNameImpl();
            dn.setDisplayName(displayName);
            displayNames = new DisplayNamesImpl();
            displayNames.add(dn);
        }
        return displayNames;
    }

    protected Icons getIcons(String smallIcon, String largeIcon) {
        IconsImpl icons = null;
        if (smallIcon.length() > 0 || largeIcon.length() > 0) {
            IconImpl i = new IconImpl();
            i.setSmallIcon(smallIcon);
            i.setLargeIcon(largeIcon);
            icons = new IconsImpl();
            icons.add(i);
        }
        return icons;
    }

    protected DescriptionGroupMetaData getDescriptionGroup(String description) {
        DescriptionGroupMetaData dg = null;
        if (description.length() > 0) {
            dg = new DescriptionGroupMetaData();
            Descriptions descriptions = getDescription(description);
            dg.setDescriptions(descriptions);
        }
        return dg;
    }

    protected DescriptionGroupMetaData getDescriptionGroup(String description, String displayName, String smallIcon,
            String largeIcon) {
        DescriptionGroupMetaData dg = null;
        if (description.length() > 0 || displayName.length() > 0 || smallIcon.length() > 0 || largeIcon.length() > 0) {
            dg = new DescriptionGroupMetaData();
            Descriptions descriptions = getDescription(description);
            if (descriptions != null)
                dg.setDescriptions(descriptions);
            DisplayNames displayNames = getDisplayName(displayName);
            if (displayNames != null)
                dg.setDisplayNames(displayNames);
            Icons icons = getIcons(smallIcon, largeIcon);
            if (icons != null)
                dg.setIcons(icons);
        }
        return dg;
    }

}
