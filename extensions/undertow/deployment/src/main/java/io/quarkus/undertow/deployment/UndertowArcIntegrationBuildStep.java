package io.quarkus.undertow.deployment;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.undertow.runtime.UndertowDeploymentRecorder;

public class UndertowArcIntegrationBuildStep {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    ServletExtensionBuildItem integrateRequestContext(BeanContainerBuildItem beanContainerBuildItem,
            UndertowDeploymentRecorder recorder) {
        return new ServletExtensionBuildItem(recorder.setupRequestScope(beanContainerBuildItem.getValue()));
    }

    @BuildStep
    void beanDefiningAnnotations(BuildProducer<BeanDefiningAnnotationBuildItem> annotations) {
        annotations.produce(new BeanDefiningAnnotationBuildItem(UndertowBuildStep.WEB_FILTER));
        annotations.produce(new BeanDefiningAnnotationBuildItem(UndertowBuildStep.WEB_SERVLET));
        annotations.produce(new BeanDefiningAnnotationBuildItem(UndertowBuildStep.WEB_LISTENER));
    }
}
