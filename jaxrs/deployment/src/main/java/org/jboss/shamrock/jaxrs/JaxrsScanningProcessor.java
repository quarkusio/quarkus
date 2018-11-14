/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.shamrock.jaxrs;

import static org.jboss.shamrock.annotations.ExecutionTime.STATIC_INIT;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.Servlet;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Providers;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.ExecutionTime;
import org.jboss.shamrock.annotations.Record;
import org.jboss.shamrock.deployment.builditem.BeanContainerBuildItem;
import org.jboss.shamrock.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.ProxyDefinitionBuildItem;
import org.jboss.shamrock.deployment.builditem.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.builditem.ReflectiveHierarchyBuildItem;
import org.jboss.shamrock.deployment.builditem.ResourceBuildItem;
import org.jboss.shamrock.deployment.builditem.SubstrateConfigBuildItem;
import org.jboss.shamrock.deployment.recording.BeanFactory;
import org.jboss.shamrock.deployment.recording.BytecodeRecorder;
import org.jboss.shamrock.jaxrs.runtime.graal.JaxrsTemplate;
import org.jboss.shamrock.jaxrs.runtime.graal.ShamrockInjectorFactory;
import org.jboss.shamrock.runtime.InjectionInstance;
import org.jboss.shamrock.undertow.DeploymentInfoBuildItem;
import org.jboss.shamrock.undertow.ServletBuildItem;
import org.jboss.shamrock.undertow.ServletContextParamBuildItem;
import org.jboss.shamrock.undertow.runtime.UndertowDeploymentTemplate;

import io.undertow.servlet.api.InstanceFactory;

/**
 * Processor that finds jax-rs classes in the deployment
 *
 * @author Stuart Douglas
 */
public class JaxrsScanningProcessor {

    private static final String JAX_RS_SERVLET_NAME = "javax.ws.rs.Application";

    private static final DotName APPLICATION_PATH = DotName.createSimple("javax.ws.rs.ApplicationPath");

    private static final DotName PATH = DotName.createSimple("javax.ws.rs.Path");
    private static final DotName DYNAMIC_FEATURE = DotName.createSimple(DynamicFeature.class.getName());

    private static final DotName XML_ROOT = DotName.createSimple("javax.xml.bind.annotation.XmlRootElement");
    private static final DotName JSONB_ANNOTATION = DotName.createSimple("javax.json.bind.annotation.JsonbAnnotation");
    private static final DotName CONTEXT = DotName.createSimple("javax.ws.rs.core.Context");

    private static final DotName[] METHOD_ANNOTATIONS = {
            DotName.createSimple("javax.ws.rs.GET"),
            DotName.createSimple("javax.ws.rs.HEAD"),
            DotName.createSimple("javax.ws.rs.DELETE"),
            DotName.createSimple("javax.ws.rs.OPTIONS"),
            DotName.createSimple("javax.ws.rs.PATCH"),
            DotName.createSimple("javax.ws.rs.POST"),
            DotName.createSimple("javax.ws.rs.PUT"),
    };


    @BuildStep
    ServletContextParamBuildItem registerProviders(List<JaxrsProviderBuildItem> providers) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < providers.size(); ++i) {
            if(i != 0) {
                sb.append(",");
            }
            sb.append(providers.get(i).getName());
        }
        return new ServletContextParamBuildItem("resteasy.providers", sb.toString());
    }

    @BuildStep
    SubstrateConfigBuildItem config() {
        return SubstrateConfigBuildItem.builder()
                .addResourceBundle("messages")
                .build();
    }

    @BuildStep
    public void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
                      BuildProducer<ProxyDefinitionBuildItem> proxyDefinition,
                      BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
                      BuildProducer<ResourceBuildItem> resource,
                      BuildProducer<ServletBuildItem> servletProducer,
                      CombinedIndexBuildItem combinedIndexBuildItem,
                      BuildProducer<ServletContextParamBuildItem> servletContextParams
    ) throws Exception {
        //this is pretty yuck, and does not really belong here, but it is needed to get the json-p
        //provider to work
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, "org.glassfish.json.JsonProviderImpl",
                "com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector",
                "com.fasterxml.jackson.databind.ser.std.SqlDateSerializer"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, ArrayList.class.getName()));
        resource.produce(new ResourceBuildItem("META-INF/services/javax.ws.rs.client.ClientBuilder"));
        IndexView index = combinedIndexBuildItem.getIndex();

        Collection<AnnotationInstance> app = index.getAnnotations(APPLICATION_PATH);
        if (app.isEmpty()) {
            return;
        }
        Collection<AnnotationInstance> xmlRoot = index.getAnnotations(XML_ROOT);
        if (!xmlRoot.isEmpty()) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, "com.sun.xml.bind.v2.ContextFactory", "com.sun.xml.internal.bind.v2.ContextFactory"));
        }
        for (DotName i : Arrays.asList(XML_ROOT, JSONB_ANNOTATION)) {
            for (AnnotationInstance anno : index.getAnnotations(i)) {
                if (anno.target().kind() == AnnotationTarget.Kind.CLASS) {
                    reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, anno.target().asClass().name().toString()));
                }
            }
        }

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
                        proxyDefinition.produce(new ProxyDefinitionBuildItem(type.toString()));
                    }
                } else {
                    //might be a framework class, which should be loadable
                    try {
                        Class<?> typeClass = Class.forName(typeName.toString());
                        if (typeClass.isInterface()) {
                            proxyDefinition.produce(new ProxyDefinitionBuildItem(typeName.toString()));
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

        AnnotationInstance appPath = app.iterator().next();
        String path = appPath.value().asString();
        String appClass = appPath.target().asClass().name().toString();
        String mappingPath;
        if (path.endsWith("/")) {
            mappingPath = path + "*";
        } else {
            mappingPath = path + "/*";
        }
        servletProducer.produce(new ServletBuildItem(JAX_RS_SERVLET_NAME, HttpServlet30Dispatcher.class.getName()).setLoadOnStartup(1).addMapping(mappingPath).setAsyncSupported(true));
        Collection<AnnotationInstance> paths = index.getAnnotations(PATH);
        if (paths != null) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, HttpServlet30Dispatcher.class.getName()));
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
                servletContextParams.produce(new ServletContextParamBuildItem(ResteasyContextParameters.RESTEASY_SCANNED_RESOURCES, sb.toString()));
            }
            servletContextParams.produce(new ServletContextParamBuildItem("resteasy.servlet.mapping.prefix", path));
            servletContextParams.produce(new ServletContextParamBuildItem("resteasy.injector.factory", ShamrockInjectorFactory.class.getName()));
            servletContextParams.produce(new ServletContextParamBuildItem(Application.class.getName(), appClass));

        }
        for (DotName annotationType : METHOD_ANNOTATIONS) {
            Collection<AnnotationInstance> instances = index.getAnnotations(annotationType);
            for (AnnotationInstance instance : instances) {
                MethodInfo method = instance.target().asMethod();
                reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem(method.returnType()));
                for (Type param : method.parameters()) {
                    if (param.kind() != Type.Kind.PRIMITIVE) {
                        reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem(param));
                    }
                }
            }
        }

        //register providers for reflection
        Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/services/" + Providers.class.getName());
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            try (InputStream in = url.openStream()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("#")) {
                        line = line.substring(line.indexOf("#"));
                    }
                    line = line.trim();
                    if (line.equals("")) continue;
                    reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, line));
                }
            }
        }


    }


    @Record(STATIC_INIT)
    @BuildStep
    void integrate(JaxrsTemplate template, BeanContainerBuildItem beanContainerBuildItem) {
        template.setupIntegration(beanContainerBuildItem.getValue());
    }
}
