package io.quarkus.arc.deployment;

import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.logging.LoggingResourceProcessor;

public class LoggingBeanSupportProcessor {

    @BuildStep
    public void discoveredComponents(BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotationProducer) {
        beanDefiningAnnotationProducer.produce(new BeanDefiningAnnotationBuildItem(
                LoggingResourceProcessor.LOGGING_FILTER, BuiltinScope.SINGLETON.getName(), false));
    }

}
