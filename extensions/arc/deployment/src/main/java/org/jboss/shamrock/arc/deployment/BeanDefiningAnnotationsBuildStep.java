package org.jboss.shamrock.arc.deployment;

import org.jboss.jandex.DotName;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.Capabilities;

public class BeanDefiningAnnotationsBuildStep {

    @BuildStep
    void beanDefiningAnnotations(Capabilities capabilities, BuildProducer<BeanDefiningAnnotationBuildItem> annotations) {
        // TODO: we should only add this when running the tests
        annotations.produce(new BeanDefiningAnnotationBuildItem(DotName.createSimple("org.junit.runner.RunWith")));
    }

}
