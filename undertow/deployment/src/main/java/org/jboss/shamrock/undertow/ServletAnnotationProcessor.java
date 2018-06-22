package org.jboss.shamrock.undertow;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.servlet.annotation.WebServlet;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.shamrock.codegen.BytecodeRecorder;
import org.jboss.shamrock.core.ArchiveContext;
import org.jboss.shamrock.core.ProcessorContext;
import org.jboss.shamrock.core.ResourceProcessor;
import org.jboss.shamrock.undertow.runtime.UndertowDeploymentTemplate;

public class ServletAnnotationProcessor implements ResourceProcessor {

    private static final DotName WEB_SERVLET = DotName.createSimple(WebServlet.class.getName());

    @Override
    public Set<DotName> getProcessedAnnotations() {
        return Collections.singleton(WEB_SERVLET);
    }

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception{
        final Index index = archiveContext.getIndex();
        List<AnnotationInstance> annotations = index.getAnnotations(WEB_SERVLET);
        if (annotations != null) {
            try (BytecodeRecorder context = processorContext.addDeploymentTask(1)) {
                UndertowDeploymentTemplate template = context.getMethodRecorder().getRecordingProxy(UndertowDeploymentTemplate.class);
                template.createDeployment("test");

                for (AnnotationInstance annotation : annotations) {
                    String name = annotation.value("name").asString();
                    template.registerServlet(null, name, annotation.target().asClass().toString());
                    String[] mappings = annotation.value("urlPatterns").asStringArray();
                    for(String m : mappings) {
                        template.addServletMapping(null, name, m);
                    }
                }
                template.deploy(null, null);
            }
        }
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
