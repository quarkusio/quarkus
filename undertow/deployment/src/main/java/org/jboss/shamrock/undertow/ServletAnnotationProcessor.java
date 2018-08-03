package org.jboss.shamrock.undertow;

import java.util.Collection;

import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.servlet.annotation.WebServlet;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.RuntimePriority;
import org.jboss.shamrock.deployment.ShamrockConfig;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;
import org.jboss.shamrock.runtime.InjectionInstance;
import org.jboss.shamrock.undertow.runtime.UndertowDeploymentTemplate;

import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.handlers.DefaultServlet;

public class ServletAnnotationProcessor implements ResourceProcessor {

    private static final DotName WEB_SERVLET = DotName.createSimple(WebServlet.class.getName());

    @Inject
    private ShamrockConfig config;

    @Inject
    private ServletDeployment deployment;

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {

        processorContext.addReflectiveClass(false, false, DefaultServlet.class.getName());
        processorContext.addReflectiveClass(false, false, "io.undertow.server.protocol.http.HttpRequestParser$$generated");

        try (BytecodeRecorder context = processorContext.addStaticInitTask(RuntimePriority.UNDERTOW_CREATE_DEPLOYMENT)) {
            UndertowDeploymentTemplate template = context.getRecordingProxy(UndertowDeploymentTemplate.class);
            template.createDeployment("test");
        }
        final IndexView index = archiveContext.getIndex();
        Collection<AnnotationInstance> annotations = index.getAnnotations(WEB_SERVLET);
        if (annotations != null && annotations.size() > 0) {
            try (BytecodeRecorder context = processorContext.addStaticInitTask(RuntimePriority.UNDERTOW_REGISTER_SERVLET)) {
                UndertowDeploymentTemplate template = context.getRecordingProxy(UndertowDeploymentTemplate.class);
                for (AnnotationInstance annotation : annotations) {
                    String name = annotation.value("name").asString();
                    AnnotationValue asyncSupported = annotation.value("asyncSupported");
                    String servletClass = annotation.target().asClass().toString();
                    processorContext.addReflectiveClass(false, false, servletClass);
                    InjectionInstance<? extends Servlet> injection = (InjectionInstance<? extends Servlet>) context.newInstanceFactory(servletClass);
                    InstanceFactory<? extends Servlet> factory = template.createInstanceFactory(injection);
                    template.registerServlet(null, name, context.classProxy(servletClass), asyncSupported != null && asyncSupported.asBoolean(), factory);
                    String[] mappings = annotation.value("urlPatterns").asStringArray();
                    for (String m : mappings) {
                        template.addServletMapping(null, name, m);
                    }
                }

                for (ServletData servlet : deployment.getServlets()) {

                    String servletClass = servlet.getServletClass();
                    InjectionInstance<? extends Servlet> injection = (InjectionInstance<? extends Servlet>) context.newInstanceFactory(servletClass);
                    InstanceFactory<? extends Servlet> factory = template.createInstanceFactory(injection);
                    template.registerServlet(null, servlet.getName(), context.classProxy(servletClass), true, factory);

                    for (String m : servlet.getMapings()) {
                        template.addServletMapping(null, servlet.getName(), m);
                    }
                }
            }
        }


        try (BytecodeRecorder context = processorContext.addStaticInitTask(RuntimePriority.UNDERTOW_DEPLOY)) {
            UndertowDeploymentTemplate template = context.getRecordingProxy(UndertowDeploymentTemplate.class);
            template.bootServletContainer(null);
        }

        try (BytecodeRecorder context = processorContext.addDeploymentTask(RuntimePriority.UNDERTOW_START)) {
            UndertowDeploymentTemplate template = context.getRecordingProxy(UndertowDeploymentTemplate.class);
            template.startUndertow(null, null, config.getConfig("http.port", "8080"));
        }
    }

    @Override
    public int getPriority() {
        return 100;
    }
}
