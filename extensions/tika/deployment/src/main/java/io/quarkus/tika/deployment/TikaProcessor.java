package io.quarkus.tika.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.resteasy.common.deployment.ResteasyJaxrsProviderBuildItem;
import io.quarkus.tika.runtime.jaxrs.TikaMessageBodyReader;

public class TikaProcessor {

    @BuildStep
    void setupTikaProvider(BuildProducer<ResteasyJaxrsProviderBuildItem> providers) {
        providers.produce(new ResteasyJaxrsProviderBuildItem(TikaMessageBodyReader.class.getName()));
    }
}
