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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Servlet;
import javax.ws.rs.ext.Providers;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.RuntimePriority;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;
import org.jboss.shamrock.jaxrs.runtime.graal.JaxrsTemplate;
import org.jboss.shamrock.jaxrs.runtime.graal.ShamrockInjectorFactory;
import org.jboss.shamrock.runtime.InjectionInstance;
import org.jboss.shamrock.undertow.runtime.UndertowDeploymentTemplate;

import io.undertow.servlet.api.InstanceFactory;

/**
 * Processor that finds jax-rs classes in the deployment
 *
 * @author Stuart Douglas
 */
public class JaxrsScanningProcessor implements ResourceProcessor {

    private static final String JAX_RS_SERVLET_NAME = "javax.ws.rs.core.Application";

    private static final DotName APPLICATION_PATH = DotName.createSimple("javax.ws.rs.ApplicationPath");

    private static final DotName PATH = DotName.createSimple("javax.ws.rs.Path");

    private static final DotName XML_ROOT = DotName.createSimple("javax.xml.bind.annotation.XmlRootElement");

    private static final DotName[] METHOD_ANNOTATIONS = {
            DotName.createSimple("javax.ws.rs.GET"),
            DotName.createSimple("javax.ws.rs.HEAD"),
            DotName.createSimple("javax.ws.rs.DELETE"),
            DotName.createSimple("javax.ws.rs.OPTIONS"),
            DotName.createSimple("javax.ws.rs.PATCH"),
            DotName.createSimple("javax.ws.rs.POST"),
            DotName.createSimple("javax.ws.rs.PUT"),
    };


    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
        //this is pretty yuck, and does not really belong here, but it is needed to get the json-p
        //provider to work
        processorContext.addReflectiveClass(true, false,"org.glassfish.json.JsonProviderImpl", "com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector");

        IndexView index = archiveContext.getIndex();
        Collection<AnnotationInstance> app = index.getAnnotations(APPLICATION_PATH);
        if (app.isEmpty()) {
            return;
        }
        Collection<AnnotationInstance> xmlRoot = index.getAnnotations(XML_ROOT);
        if (!xmlRoot.isEmpty()) {
            processorContext.addReflectiveClass(true, false,"com.sun.xml.bind.v2.ContextFactory", "com.sun.xml.internal.bind.v2.ContextFactory");
        }
        AnnotationInstance appPath = app.iterator().next();
        String path = appPath.value().asString();
        try (BytecodeRecorder recorder = processorContext.addStaticInitTask(RuntimePriority.JAXRS_DEPLOYMENT)) {
            UndertowDeploymentTemplate undertow = recorder.getRecordingProxy(UndertowDeploymentTemplate.class);
            InjectionInstance<? extends Servlet> instanceFactory = (InjectionInstance<? extends Servlet>) recorder.newInstanceFactory(HttpServlet30Dispatcher.class.getName());
            InstanceFactory<? extends Servlet> factory = undertow.createInstanceFactory(instanceFactory);
            undertow.registerServlet(null, JAX_RS_SERVLET_NAME, recorder.classProxy(HttpServlet30Dispatcher.class.getName()), true, factory);
            undertow.addServletMapping(null, JAX_RS_SERVLET_NAME, path + "/*");
            Collection<AnnotationInstance> paths = index.getAnnotations(PATH);
            if (paths != null) {
                processorContext.addReflectiveClass(false,  false, HttpServlet30Dispatcher.class.getName());
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (AnnotationInstance annotation : paths) {
                    if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(",");
                        }
                        String className = annotation.target().asClass().name().toString();
                        sb.append(className);
                        processorContext.addReflectiveClass(true, true, className);
                    }
                }

                if(sb.length() > 0) {
                    undertow.addServletContextParameter(null, ResteasyContextParameters.RESTEASY_SCANNED_RESOURCES, sb.toString());
                }
                undertow.addServletContextParameter(null, "resteasy.servlet.mapping.prefix", path);
                undertow.addServletContextParameter(null, "resteasy.injector.factory", ShamrockInjectorFactory.class.getName());
                for (String i : loadProviders()) {
                    processorContext.addReflectiveClass(true, true, i);
                }
            }
        }
        for (DotName annotationType : METHOD_ANNOTATIONS) {
            Collection<AnnotationInstance> instances = index.getAnnotations(annotationType);
            for (AnnotationInstance instance : instances) {
                MethodInfo method = instance.target().asMethod();
                if (method.returnType().kind() == Type.Kind.CLASS) {
                    String className = method.returnType().asClassType().name().toString();
                    if (!className.equals(String.class.getName())) {
                        processorContext.addReflectiveClass(true, true, className);
                    }
                }
            }
        }

        try (BytecodeRecorder recorder = processorContext.addStaticInitTask(RuntimePriority.JAXRS_DEPLOYMENT)) {
            JaxrsTemplate jaxrs = recorder.getRecordingProxy(JaxrsTemplate.class);
            jaxrs.setupIntegration(null);
        }
    }

    @Override
    public int getPriority() {
        return RuntimePriority.JAXRS_DEPLOYMENT;
    }

    private Set<String> loadProviders() {

        final Set<String> providers = new HashSet<>();
        try {
            Enumeration<URL> en;
            en = Thread.currentThread().getContextClassLoader().getResources("META-INF/services/" + Providers.class.getName());

            while (en.hasMoreElements()) {
                final URL url = en.nextElement();
                InputStream is;
                if (System.getSecurityManager() == null) {
                    is = url.openStream();
                } else {
                    is = AccessController.doPrivileged(new PrivilegedExceptionAction<InputStream>() {
                        @Override
                        public InputStream run() throws IOException {
                            return url.openStream();
                        }
                    });
                }

                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.equals("")) continue;
                        providers.add(line);
                    }
                } finally {
                    is.close();
                }
            }
            return Collections.unmodifiableSet(providers);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
