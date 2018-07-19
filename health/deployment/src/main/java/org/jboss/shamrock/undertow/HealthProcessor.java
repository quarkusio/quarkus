package org.jboss.shamrock.undertow;

import javax.servlet.Servlet;

import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.RuntimePriority;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;
import org.jboss.shamrock.health.runtime.HealthServlet;
import org.jboss.shamrock.undertow.runtime.UndertowDeploymentTemplate;

import io.smallrye.health.SmallRyeHealthReporter;
import io.undertow.servlet.api.InstanceFactory;

public class HealthProcessor implements ResourceProcessor {

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
        try (BytecodeRecorder context = processorContext.addStaticInitTask(RuntimePriority.HEALTH_DEPLOYMENT)) {
            UndertowDeploymentTemplate template = context.getRecordingProxy(UndertowDeploymentTemplate.class);
            InstanceFactory<?> factory = template.createInstanceFactory(context.newInstanceFactory(HealthServlet.class.getName()));
            template.registerServlet(null, "health", HealthServlet.class, true, (InstanceFactory<? extends Servlet>) factory);
            template.addServletMapping(null, "health", "/health");
        }
        processorContext.addAdditionalBean(SmallRyeHealthReporter.class);
        processorContext.addAdditionalBean(HealthServlet.class);
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
