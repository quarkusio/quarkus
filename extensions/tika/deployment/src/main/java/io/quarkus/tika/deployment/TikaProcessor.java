package io.quarkus.tika.deployment;

import java.util.Set;

import org.apache.tika.detect.Detector;
import org.apache.tika.detect.EncodingDetector;
import org.apache.tika.parser.Parser;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
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
    public void produceTikaParsersParsers(BuildProducer<SubstrateResourceBuildItem> resource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) throws Exception {
        produceServiceLoaderResources(resource, reflectiveClass, Parser.class.getName());
    }

    @BuildStep
    public void produceTikaParsersDetectors(BuildProducer<SubstrateResourceBuildItem> resource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) throws Exception {
        produceServiceLoaderResources(resource, reflectiveClass, Detector.class.getName());
    }

    @BuildStep
    public void produceTikaParsersEncodingDetectors(BuildProducer<SubstrateResourceBuildItem> resource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) throws Exception {
        produceServiceLoaderResources(resource, reflectiveClass, EncodingDetector.class.getName());
    }

    private void produceServiceLoaderResources(BuildProducer<SubstrateResourceBuildItem> resource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            String serviceLoaderResourceName) throws Exception {
        resource.produce(new SubstrateResourceBuildItem("META-INF/services/" + serviceLoaderResourceName));
        Set<String> availableProviderClasses = ServiceUtil.classNamesNamedIn(getClass().getClassLoader(),
                "META-INF/services/" + serviceLoaderResourceName);
        for (String providerClass : availableProviderClasses) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, providerClass));
        }
    }

}
