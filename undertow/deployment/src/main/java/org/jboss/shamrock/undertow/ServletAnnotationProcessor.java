package org.jboss.shamrock.undertow;

import java.util.List;

import javax.servlet.annotation.WebServlet;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.shamrock.codegen.BytecodeRecorder;
import org.jboss.shamrock.core.ArchiveContext;
import org.jboss.shamrock.core.ProcessorContext;
import org.jboss.shamrock.core.ResourceProcessor;
import org.jboss.shamrock.core.RuntimePriority;
import org.jboss.shamrock.undertow.runtime.UndertowDeploymentTemplate;

import io.undertow.servlet.handlers.DefaultServlet;

public class ServletAnnotationProcessor implements ResourceProcessor {

    private static final DotName WEB_SERVLET = DotName.createSimple(WebServlet.class.getName());

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {

        processorContext.addReflectiveClass(DefaultServlet.class.getName());
        processorContext.addReflectiveClass("io.undertow.server.protocol.http.HttpRequestParser$$generated");

        try (BytecodeRecorder context = processorContext.addDeploymentTask(RuntimePriority.UNDERTOW_CREATE_DEPLOYMENT)) {
            UndertowDeploymentTemplate template = context.getRecordingProxy(UndertowDeploymentTemplate.class);
            template.createDeployment("test");
        }
        final Index index = archiveContext.getIndex();
        List<AnnotationInstance> annotations = index.getAnnotations(WEB_SERVLET);
        if (annotations != null) {
            try (BytecodeRecorder context = processorContext.addDeploymentTask(RuntimePriority.UNDERTOW_REGISTER_SERVLET)) {
                UndertowDeploymentTemplate template = context.getRecordingProxy(UndertowDeploymentTemplate.class);
                for (AnnotationInstance annotation : annotations) {
                    String name = annotation.value("name").asString();
                    AnnotationValue asyncSupported = annotation.value("asyncSupported");
                    String servletClass = annotation.target().asClass().toString();

                    context.newInstanceFactory(servletClass, "injector");
                    template.createInstanceFactory(null);
                    template.registerServlet(null, name, servletClass, asyncSupported != null && asyncSupported.asBoolean(), null);
                    String[] mappings = annotation.value("urlPatterns").asStringArray();
                    for (String m : mappings) {
                        template.addServletMapping(null, name, m);
                    }
                }
            }
        }

        try (BytecodeRecorder context = processorContext.addDeploymentTask(RuntimePriority.UNDERTOW_START)) {
            UndertowDeploymentTemplate template = context.getRecordingProxy(UndertowDeploymentTemplate.class);
            template.deploy(null, null);
        }
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
