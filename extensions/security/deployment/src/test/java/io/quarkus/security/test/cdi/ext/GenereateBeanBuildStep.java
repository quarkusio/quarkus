package io.quarkus.security.test.cdi.ext;

import javax.annotation.security.DenyAll;
import javax.enterprise.context.ApplicationScoped;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;

public class GenereateBeanBuildStep {

    @BuildStep
    public void generateSecuredBean(BuildProducer<GeneratedBeanBuildItem> generatedBeans) {

        ClassOutput beansClassOutput = new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                generatedBeans.produce(new GeneratedBeanBuildItem(name, data));
            }
        };
        ClassCreator creator = ClassCreator.builder().className("io.quarkus.security.test.GeneratedBean")
                .interfaces(io.quarkus.security.test.cdi.ext.GeneratedBean.class)
                .classOutput(beansClassOutput).build();

        creator.addAnnotation(ApplicationScoped.class);

        MethodCreator method = creator.getMethodCreator("secured", void.class);
        method.returnValue(method.loadNull());
        method.addAnnotation(DenyAll.class);

        creator.close();
    }
}
