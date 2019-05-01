package io.quarkus.tika.deployment;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

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

    private static final Set<String> NATIVE_READY_PARSERS = Arrays.stream(new String[] {
            "org.apache.tika.parser.txt.TXTParser",
            "org.apache.tika.parser.odf.OpenDocumentParser"
    }).collect(Collectors.toSet());

    @BuildStep
    void produceTikaJaxrsProvider(BuildProducer<ResteasyJaxrsProviderBuildItem> providers) {
        providers.produce(new ResteasyJaxrsProviderBuildItem(TikaMessageBodyReader.class.getName()));
    }

    @BuildStep
    public void produceTikaCoreResources(BuildProducer<SubstrateResourceBuildItem> resource) throws Exception {
        resource.produce(new SubstrateResourceBuildItem("org/apache/tika/mime/tika-mimetypes.xml"));
        resource.produce(new SubstrateResourceBuildItem("org/apache/tika/parser/external/tika-external-parsers.xml"));
    }

    @BuildStep
    public void produceTikaParsersProviders(BuildProducer<SubstrateResourceBuildItem> resource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) throws Exception {
        produceTikaServiceProviders(resource, reflectiveClass, Parser.class.getName());
        produceTikaServiceProviders(resource, reflectiveClass, Detector.class.getName());
        produceTikaServiceProviders(resource, reflectiveClass, EncodingDetector.class.getName());
    }

    private void produceTikaServiceProviders(BuildProducer<SubstrateResourceBuildItem> resource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            String serviceProviderName) throws Exception {
        resource.produce(new SubstrateResourceBuildItem("META-INF/services/" + serviceProviderName));
        Set<String> availableProviderClasses = ServiceUtil.classNamesNamedIn(getClass().getClassLoader(),
                "META-INF/services/" + serviceProviderName);
        for (String providerClass : availableProviderClasses) {
            if (!Parser.class.getName().equals(serviceProviderName) || NATIVE_READY_PARSERS.contains(providerClass)) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, providerClass));
            }
        }
    }

}
