package io.quarkus.tika.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.resteasy.common.deployment.ResteasyJaxrsProviderBuildItem;
import io.quarkus.tika.runtime.jaxrs.TikaMessageBodyReader;

public class TikaProcessor {

    @BuildStep
    void produceTikaProvider(BuildProducer<ResteasyJaxrsProviderBuildItem> providers) {
        providers.produce(new ResteasyJaxrsProviderBuildItem(TikaMessageBodyReader.class.getName()));
    }

    @BuildStep
    public void produceTikaCoreResources(BuildProducer<SubstrateResourceBuildItem> resource) throws Exception {
        resource.produce(new SubstrateResourceBuildItem("org/apache/tika/mime/tika-mimetypes.xml"));
        resource.produce(new SubstrateResourceBuildItem("org/apache/tika/parser/external/tika-external-parsers.xml"));
    }

    @BuildStep
    public void produceTikaParsersResources(BuildProducer<SubstrateResourceBuildItem> resource) throws Exception {
        resource.produce(new SubstrateResourceBuildItem("META-INF/services/org.apache.tika.parser.Parser"));
        resource.produce(new SubstrateResourceBuildItem("META-INF/services/org.apache.tika.detect.Detector"));
        resource.produce(new SubstrateResourceBuildItem("META-INF/services/org.apache.tika.detect.EncodingDetector"));
    }

}
