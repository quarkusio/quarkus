package org.jboss.shamrock.arc.deployment;

import org.jboss.jandex.DotName;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.Capabilities;
import org.jboss.shamrock.undertow.UndertowBuildStep;

public class BeanDefiningAnnotationsBuildStep {

    @BuildStep
    void beanDefiningAnnotations(Capabilities capabilities, BuildProducer<BeanDefiningAnnotationBuildItem> annotations) {
        if (capabilities.isCapabilityPresent(Capabilities.UNDERTOW)) {
            // Arc integration depends on undertow so we want to avoid cyclic dependencies
            annotations.produce(new BeanDefiningAnnotationBuildItem(UndertowBuildStep.WEB_FILTER));
            annotations.produce(new BeanDefiningAnnotationBuildItem(UndertowBuildStep.WEB_SERVLET));
            annotations.produce(new BeanDefiningAnnotationBuildItem(UndertowBuildStep.WEB_LISTENER));
        }

        // TODO: we should only add this when running the tests
        annotations.produce(new BeanDefiningAnnotationBuildItem(DotName.createSimple("org.junit.runner.RunWith")));
    }

}
