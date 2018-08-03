package org.jboss.shamrock.weld.deployment;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.inject.Inject;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.RuntimePriority;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;
import org.jboss.shamrock.weld.runtime.WeldDeploymentTemplate;

import io.smallrye.config.inject.ConfigProducer;

public class WeldAnnotationProcessor implements ResourceProcessor {

    @Inject
    private WeldDeployment weldDeployment;

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
        //make config injectable
        weldDeployment.addAdditionalBean(ConfigProducer.class);
        IndexView index = archiveContext.getIndex();
        try (BytecodeRecorder recorder = processorContext.addStaticInitTask(RuntimePriority.WELD_DEPLOYMENT)) {
            WeldDeploymentTemplate template = recorder.getRecordingProxy(WeldDeploymentTemplate.class);
            SeContainerInitializer init = template.createWeld();
            for (ClassInfo cl : index.getKnownClasses()) {
                String name = cl.name().toString();
                //TODO: massive hack
                //the runtime runner picks up the classes created by the maven plugin
                if (!name.startsWith("org.jboss.shamrock.deployment") && !name.startsWith("org.jboss.shamrock.runner")) {
                    template.addClass(init, recorder.classProxy(name));
                    processorContext.addReflectiveClass(true, true, name);
                }
            }
            for (Class<?> clazz : weldDeployment.getAdditionalBeans()) {
                template.addClass(init, clazz);
            }
            for (Class<?> clazz : weldDeployment.getInterceptors()) {
                template.addInterceptor(init, clazz);
            }
            SeContainer weld = template.doBoot(null, init);
            template.setupInjection(null, weld);
        }

    }

    @Override
    public int getPriority() {
        return RuntimePriority.WELD_DEPLOYMENT;
    }
}
