package org.acme.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.restclient.deployment.RestClientAnnotationProviderBuildItem;
import org.acme.CustomHeader1Filter;
import org.acme.CustomHeader2Filter;


public class ExtensionBuildStep {

    @BuildStep
    void registerProvider(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<RestClientAnnotationProviderBuildItem> restAnnotationProvider) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(CustomHeader1Filter.class));
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(CustomHeader2Filter.class));
        
        restAnnotationProvider.produce(new RestClientAnnotationProviderBuildItem("org.acme.CustomHeader1",
                CustomHeader1Filter.class));
        restAnnotationProvider.produce(new RestClientAnnotationProviderBuildItem("org.acme.CustomHeader2",
                CustomHeader2Filter.class));
    }
}
