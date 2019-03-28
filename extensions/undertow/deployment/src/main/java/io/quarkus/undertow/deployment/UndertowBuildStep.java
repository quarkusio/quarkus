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

package io.quarkus.undertow.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static io.undertow.servlet.api.SecurityInfo.EmptyRoleSemantic.DENY;
import static io.undertow.servlet.api.SecurityInfo.EmptyRoleSemantic.PERMIT;
import static javax.servlet.DispatcherType.REQUEST;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.jboss.annotation.javaee.Descriptions;
import org.jboss.annotation.javaee.DisplayNames;
import org.jboss.annotation.javaee.Icons;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
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
import org.jboss.metadata.parser.servlet.WebMetaDataParser;
import org.jboss.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.property.PropertyReplacers;
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
import io.quarkus.arc.processor.ContextRegistrar;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentConfigFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ObjectSubstitutionBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateConfigBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.undertow.runtime.HttpConfig;
import io.quarkus.undertow.runtime.HttpSessionContext;
import io.quarkus.undertow.runtime.ServletProducer;
import io.quarkus.undertow.runtime.ServletSecurityInfoProxy;
import io.quarkus.undertow.runtime.ServletSecurityInfoSubstitution;
import io.quarkus.undertow.runtime.UndertowDeploymentTemplate;
import io.undertow.Undertow;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.HttpMethodSecurityInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.api.ServletSecurityInfo;
import io.undertow.servlet.handlers.DefaultServlet;

public class UndertowBuildStep {

    public static final DotName WEB_FILTER = DotName.createSimple(WebFilter.class.getName());
    public static final DotName WEB_LISTENER = DotName.createSimple(WebListener.class.getName());
    public static final DotName WEB_SERVLET = DotName.createSimple(WebServlet.class.getName());
    public static final DotName RUN_AS = DotName.createSimple(RunAs.class.getName());
    public static final DotName DECLARE_ROLES = DotName.createSimple(DeclareRoles.class.getName());
    public static final DotName MULTIPART_CONFIG = DotName.createSimple(MultipartConfig.class.getName());
    public static final DotName SERVLET_SECURITY = DotName.createSimple(ServletSecurity.class.getName());
    public static final String WEB_XML = "META-INF/web.xml";

    @Inject
    CombinedIndexBuildItem combinedIndexBuildItem;

    @BuildStep
    @Record(RUNTIME_INIT)
    public ServiceStartBuildItem boot(UndertowDeploymentTemplate template,
            ServletDeploymentManagerBuildItem servletDeploymentManagerBuildItem,
            List<HttpHandlerWrapperBuildItem> wrappers,
            ShutdownContextBuildItem shutdown,
            Consumer<UndertowBuildItem> undertowProducer,
            LaunchModeBuildItem launchMode,
            HttpConfig config) throws Exception {
        RuntimeValue<Undertow> ut = template.startUndertow(shutdown, servletDeploymentManagerBuildItem.getDeploymentManager(),
                config, wrappers.stream().map(HttpHandlerWrapperBuildItem::getValue).collect(Collectors.toList()),
                launchMode.getLaunchMode());
        undertowProducer.accept(new UndertowBuildItem(ut));
        return new ServiceStartBuildItem("undertow");
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
    HotDeploymentConfigFileBuildItem configFile() {
        return new HotDeploymentConfigFileBuildItem(WEB_XML);
    }

    @BuildStep
    WebMetadataBuildItem createWebMetadata(ApplicationArchivesBuildItem applicationArchivesBuildItem,
            Consumer<AdditionalBeanBuildItem> additionalBeanBuildItemConsumer) throws Exception {

        WebMetaData result;
        Path webXml = applicationArchivesBuildItem.getRootArchive().getChildPath(WEB_XML);
        if (webXml != null) {
            Set<String> additionalBeans = new HashSet<>();

            final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            MetaDataElementParser.DTDInfo dtdInfo = new MetaDataElementParser.DTDInfo();
            inputFactory.setXMLResolver(dtdInfo);
            try (FileInputStream in = new FileInputStream(webXml.toFile())) {
                final XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(in);
                result = WebMetaDataParser.parse(xmlReader, dtdInfo, PropertyReplacers.noop());
            }
            if (result.getServlets() != null) {
                for (ServletMetaData i : result.getServlets()) {
                    additionalBeans.add(i.getServletClass());
                }
            }
            if (result.getFilters() != null) {
                for (FilterMetaData i : result.getFilters()) {
                    additionalBeans.add(i.getFilterClass());
                }
            }
            if (result.getListeners() != null) {
                for (ListenerMetaData i : result.getListeners()) {
                    additionalBeans.add(i.getListenerClass());
                }
            }
            additionalBeanBuildItemConsumer
                    .accept(AdditionalBeanBuildItem.builder().setUnremovable().addBeanClasses(additionalBeans).build());
        } else {
            result = new WebMetaData();
        }
        return new WebMetadataBuildItem(result);
    }

    @BuildStep
    public void kubernetes(HttpConfig config, BuildProducer<KubernetesPortBuildItem> portProducer) {
        portProducer.produce(new KubernetesPortBuildItem(config.port, "http"));
    }

    @Record(STATIC_INIT)
    @BuildStep()
    public ServletDeploymentManagerBuildItem build(ApplicationArchivesBuildItem applicationArchivesBuildItem,
            List<ServletBuildItem> servlets,
            List<FilterBuildItem> filters,
            List<ListenerBuildItem> listeners,
            List<ServletInitParamBuildItem> initParams,
            List<ServletContextAttributeBuildItem> contextParams,
            UndertowDeploymentTemplate template, RecorderContext context,
            List<ServletExtensionBuildItem> extensions,
            BeanContainerBuildItem bc,
            WebMetadataBuildItem webMetadataBuildItem,
            BuildProducer<ObjectSubstitutionBuildItem> substitutions,
            Consumer<ReflectiveClassBuildItem> reflectiveClasses,
            LaunchModeBuildItem launchMode,
            ShutdownContextBuildItem shutdownContext) throws Exception {

        ObjectSubstitutionBuildItem.Holder holder = new ObjectSubstitutionBuildItem.Holder(ServletSecurityInfo.class,
                ServletSecurityInfoProxy.class, ServletSecurityInfoSubstitution.class);
        substitutions.produce(new ObjectSubstitutionBuildItem(holder));
        reflectiveClasses.accept(new ReflectiveClassBuildItem(false, false, DefaultServlet.class.getName(),
                "io.undertow.server.protocol.http.HttpRequestParser$$generated"));

        //we need to check for web resources in order to get welcome files to work
        //this kinda sucks
        Set<String> knownFiles = new HashSet<>();
        Set<String> knownDirectories = new HashSet<>();
        for (ApplicationArchive i : applicationArchivesBuildItem.getAllApplicationArchives()) {
            Path resource = i.getChildPath("META-INF/resources");
            if (resource != null && Files.exists(resource)) {
                Files.walk(resource).forEach(new Consumer<Path>() {
                    @Override
                    public void accept(Path path) {
                        // Skip META-INF/resources entry
                        if (resource.equals(path)) {
                            return;
                        }
                        Path rel = resource.relativize(path);
                        if (Files.isDirectory(rel)) {
                            knownDirectories.add(rel.toString());
                        } else {
                            knownFiles.add(rel.toString());
                        }
                    }
                });
            }
        }

        RuntimeValue<DeploymentInfo> deployment = template.createDeployment("test", knownFiles, knownDirectories,
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
                RuntimeValue<ServletInfo> sref = template.registerServlet(deployment, servlet.getServletName(),
                        context.classProxy(servlet.getServletClass()),
                        servlet.isAsyncSupported(),
                        servlet.getLoadOnStartupInt(),
                        bc.getValue(),
                        params,
                        null);
                if (servlet.getInitParam() != null) {
                    for (ParamValueMetaData init : servlet.getInitParam()) {
                        template.addServletInitParam(sref, init.getParamName(), init.getParamValue());
                    }
                }
                if (servlet.getMultipartConfig() != null) {
                    template.setMultipartConfig(sref, servlet.getMultipartConfig().getLocation(),
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
                            template.setSecurityInfo(sref, securityInfo);
                        }
                        if (servlet.getSecurityRoleRefs() != null) {
                            for (final SecurityRoleRefMetaData ref : servlet.getSecurityRoleRefs()) {
                                template.addSecurityRoleRef(sref, ref.getRoleName(), ref.getRoleLink());
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
                    template.addServletMapping(deployment, mapping.getServletName(), m);
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
                RuntimeValue<FilterInfo> sref = template.registerFilter(deployment,
                        filter.getFilterName(),
                        context.classProxy(filter.getFilterClass()),
                        filter.isAsyncSupported(),
                        bc.getValue(),
                        params, null);
                if (filter.getInitParam() != null) {
                    for (ParamValueMetaData init : filter.getInitParam()) {
                        template.addFilterInitParam(sref, init.getParamName(), init.getParamValue());
                    }
                }
            }
        }
        if (webMetaData.getFilterMappings() != null) {
            for (FilterMappingMetaData mapping : webMetaData.getFilterMappings()) {
                for (String m : mapping.getUrlPatterns()) {
                    if (mapping.getDispatchers() == null || mapping.getDispatchers().isEmpty()) {
                        template.addFilterURLMapping(deployment, mapping.getFilterName(), m, REQUEST);
                    } else {

                        for (DispatcherType dispatcher : mapping.getDispatchers()) {
                            template.addFilterURLMapping(deployment, mapping.getFilterName(), m,
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
                template.registerListener(deployment, context.classProxy(listener.getListenerClass()), bc.getValue());
            }
        }

        for (ServletBuildItem servlet : servlets) {
            String servletClass = servlet.getServletClass();
            if (servlet.getLoadOnStartup() == 0) {
                reflectiveClasses.accept(new ReflectiveClassBuildItem(false, false, servlet.getServletClass()));
            }
            template.registerServlet(deployment, servlet.getName(), context.classProxy(servletClass),
                    servlet.isAsyncSupported(), servlet.getLoadOnStartup(), bc.getValue(), servlet.getInitParams(),
                    servlet.getInstanceFactory());

            for (String m : servlet.getMappings()) {
                template.addServletMapping(deployment, servlet.getName(), m);
            }
        }

        for (FilterBuildItem filter : filters) {
            String filterClass = filter.getFilterClass();
            reflectiveClasses.accept(new ReflectiveClassBuildItem(false, false, filterClass));
            template.registerFilter(deployment, filter.getName(), context.classProxy(filterClass), filter.isAsyncSupported(),
                    bc.getValue(), filter.getInitParams(), filter.getInstanceFactory());
            for (FilterBuildItem.FilterMappingInfo m : filter.getMappings()) {
                if (m.getMappingType() == FilterBuildItem.FilterMappingInfo.MappingType.URL) {
                    template.addFilterURLMapping(deployment, filter.getName(), m.getMapping(), m.getDispatcher());
                } else {
                    template.addFilterServletNameMapping(deployment, filter.getName(), m.getMapping(), m.getDispatcher());
                }
            }
        }
        for (ServletInitParamBuildItem i : initParams) {
            template.addServltInitParameter(deployment, i.getKey(), i.getValue());
        }
        for (ServletContextAttributeBuildItem i : contextParams) {
            template.addServletContextAttribute(deployment, i.getKey(), i.getValue());
        }
        for (ServletExtensionBuildItem i : extensions) {
            template.addServletExtension(deployment, i.getValue());
        }
        for (ListenerBuildItem i : listeners) {
            reflectiveClasses.accept(new ReflectiveClassBuildItem(false, false, i.getListenerClass()));
            template.registerListener(deployment, context.classProxy(i.getListenerClass()), bc.getValue());
        }
        return new ServletDeploymentManagerBuildItem(template.bootServletContainer(deployment, bc.getValue()));

    }

    @BuildStep
    SubstrateResourceBuildItem registerSubstrateResources(ArchiveRootBuildItem root,
            ApplicationArchivesBuildItem applicationArchivesBuildItem) throws IOException {
        List<String> res = new ArrayList<>();
        Path resources = applicationArchivesBuildItem.getRootArchive().getChildPath("META-INF/resources");
        if (resources != null) {
            Files.walk(resources).forEach(new Consumer<Path>() {
                @Override
                public void accept(Path path) {
                    if (!Files.isDirectory(path)) {
                        res.add(root.getPath().relativize(path).toString());
                    }
                }
            });
        }
        return new SubstrateResourceBuildItem(res);
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
