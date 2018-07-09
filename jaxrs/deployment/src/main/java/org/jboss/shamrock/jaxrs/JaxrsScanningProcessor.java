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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.ext.Providers;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrapClasses;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.shamrock.codegen.BytecodeRecorder;
import org.jboss.shamrock.core.ArchiveContext;
import org.jboss.shamrock.core.ProcessorContext;
import org.jboss.shamrock.core.ResourceProcessor;
import org.jboss.shamrock.core.RuntimePriority;
import org.jboss.shamrock.injection.InjectionInstance;
import org.jboss.shamrock.undertow.runtime.UndertowDeploymentTemplate;

/**
 * Processor that finds jax-rs classes in the deployment
 *
 * @author Stuart Douglas
 */
public class JaxrsScanningProcessor implements ResourceProcessor {

    private static final String JAX_RS_SERVLET_NAME = "javax.ws.rs.core.Application";

    private static final DotName APPLICATION_PATH = DotName.createSimple("javax.ws.rs.ApplicationPath");
    private static final DotName PATH = DotName.createSimple("javax.ws.rs.Path");

    public static final Set<String> BOOT_CLASSES = new HashSet<String>();
    public static final Set<String> BUILTIN_PROVIDERS;

    static {
        Collections.addAll(BOOT_CLASSES, ResteasyBootstrapClasses.BOOTSTRAP_CLASSES);

        final Set<String> providers = new HashSet<>();
        try {
            Enumeration<URL> en;
            if (System.getSecurityManager() == null) {
                en = Thread.currentThread().getContextClassLoader().getResources("META-INF/services/" + Providers.class.getName());
            } else {
                en = AccessController.doPrivileged(new PrivilegedExceptionAction<Enumeration<URL>>() {
                    @Override
                    public Enumeration<URL> run() throws IOException {
                        return Thread.currentThread().getContextClassLoader().getResources("META-INF/services/" + Providers.class.getName());
                    }
                });
            }

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
            BUILTIN_PROVIDERS = Collections.unmodifiableSet(providers);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {

        Index index = archiveContext.getIndex();
        List<AnnotationInstance> app = index.getAnnotations(APPLICATION_PATH);
        if (app == null || app.isEmpty()) {
            return;
        }
        AnnotationInstance appPath = app.get(0);
        String path = appPath.value().asString();
        try (BytecodeRecorder recorder = processorContext.addDeploymentTask(RuntimePriority.JAXRS_DEPLOYMENT)) {
            UndertowDeploymentTemplate undertow = recorder.getRecordingProxy(UndertowDeploymentTemplate.class);
            InjectionInstance<?> instanceFactory = recorder.newInstanceFactory(HttpServlet30Dispatcher.class.getName());
            undertow.createInstanceFactory(instanceFactory);
            undertow.registerServlet(null, JAX_RS_SERVLET_NAME, HttpServlet30Dispatcher.class.getName(), true, null);
            undertow.addServletMapping(null, JAX_RS_SERVLET_NAME, path + "/*");
            List<AnnotationInstance> paths = index.getAnnotations(PATH);
            if (paths != null) {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (AnnotationInstance annotation : paths) {
                    if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(",");
                        }
                        processorContext.addReflectiveClass(HttpServlet30Dispatcher.class.getName());
                        String className = annotation.target().asClass().name().toString();
                        sb.append(className);
                        processorContext.addReflectiveClass(className);
                    }
                }

                undertow.addServletContextParameter(null, ResteasyContextParameters.RESTEASY_SCANNED_RESOURCES, sb.toString());
                undertow.addServletContextParameter(null, "resteasy.servlet.mapping.prefix", path);
                processorContext.addReflectiveClass(HttpServlet30Dispatcher.class.getName());
                for (String i : BUILTIN_PROVIDERS) {
                    processorContext.addReflectiveClass(i);
                }
            }

        }
    }

    @Override
    public int getPriority() {
        return RuntimePriority.JAXRS_DEPLOYMENT;
    }

}
