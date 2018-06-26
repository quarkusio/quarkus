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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Application;

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
import org.jboss.shamrock.undertow.runtime.UndertowDeploymentTemplate;

/**
 * Processor that finds jax-rs classes in the deployment
 *
 * @author Stuart Douglas
 */
public class JaxrsScanningProcessor implements ResourceProcessor {

    private static final String JAX_RS_SERVLET_NAME = "javax.ws.rs.core.Application";

    private static final DotName APPLICATION_PATH = DotName.createSimple("javax.ws.rs.ApplicationPath");
    private static final DotName DECORATOR = DotName.createSimple("javax.decorator.Decorator");

    public static final DotName APPLICATION = DotName.createSimple(Application.class.getName());
    private static final String ORG_APACHE_CXF = "org.apache.cxf";


    public static final Set<String> BOOT_CLASSES = new HashSet<String>();

    static {
        Collections.addAll(BOOT_CLASSES, ResteasyBootstrapClasses.BOOTSTRAP_CLASSES);
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
            UndertowDeploymentTemplate undertow = recorder.getMethodRecorder().getRecordingProxy(UndertowDeploymentTemplate.class);
            undertow.registerServlet(null, JAX_RS_SERVLET_NAME, HttpServlet30Dispatcher.class.getName(), true);
            undertow.addServletMapping(null, JAX_RS_SERVLET_NAME, path + "/*");
            List<AnnotationInstance> paths = index.getAnnotations(JaxrsAnnotations.PATH.getDotName());
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
                        sb.append(annotation.target().asClass().name());
                    }
                }

                undertow.addServletContextParameter(null, ResteasyContextParameters.RESTEASY_SCANNED_RESOURCES, sb.toString());
                undertow.addServletContextParameter(null, "resteasy.servlet.mapping.prefix", path);
            }

        }
    }

    @Override
    public int getPriority() {
        return RuntimePriority.JAXRS_DEPLOYMENT;
    }

}
