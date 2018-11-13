package org.jboss.shamrock.opentracing;

import java.lang.reflect.Method;

import javax.enterprise.inject.spi.ObserverMethod;
import javax.inject.Inject;

import io.opentracing.contrib.interceptors.OpenTracingInterceptor;
import org.eclipse.microprofile.opentracing.Traced;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.BeanDeployment;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.RuntimePriority;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;
import org.jboss.shamrock.opentracing.runtime.TracerProducer;
import org.jboss.shamrock.opentracing.runtime.TracingDeploymentTemplate;

public class OpentracingProcessor implements ResourceProcessor {

    @Inject
    BeanDeployment beanDeployment;

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
        System.err.println( "PROCESS OPENTRACING");
        this.beanDeployment.addAdditionalBean(OpenTracingInterceptor.class);
        this.beanDeployment.addAdditionalBean(TracerProducer.class);
        processorContext.addReflectiveClass(false, false, Traced.class.getName());

        Method isAsync = ObserverMethod.class.getMethod("isAsync");
        processorContext.addReflectiveMethod(isAsync);

        try (BytecodeRecorder recorder = processorContext.addStaticInitTask(RuntimePriority.JAXRS_DEPLOYMENT + 1)) {
            TracingDeploymentTemplate tracing = recorder.getRecordingProxy(TracingDeploymentTemplate.class);
            tracing.registerTracer();
            tracing.integrateJaxrs(null);
        }
    }

    @Override
    public int getPriority() {
        return 0;
    }
}
