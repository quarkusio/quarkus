package io.quarkus.tika.deployment;

import java.util.HashSet;
import java.util.Set;

import org.apache.tika.detect.Detector;
import org.apache.tika.detect.EncodingDetector;
import org.apache.tika.parser.Parser;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.resteasy.common.deployment.ResteasyJaxrsProviderBuildItem;
import io.quarkus.tika.runtime.jaxrs.TikaMessageBodyReader;

public class TikaProcessor {

    private static final Set<String> RUNTIME_INITIALIZED_CLASSES;
    static {
        RUNTIME_INITIALIZED_CLASSES = new HashSet<>();
        RUNTIME_INITIALIZED_CLASSES.add("org.apache.tika.parser.pdf.PDFParser");
    }

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

    @BuildStep
    public void produceRuntimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> resource) {
        for (String className : RUNTIME_INITIALIZED_CLASSES) {
            resource.produce(new RuntimeInitializedClassBuildItem(className));
        }
    }

    private void produceServiceLoaderResources(BuildProducer<SubstrateResourceBuildItem> resource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            String serviceProviderName) throws Exception {
        resource.produce(new SubstrateResourceBuildItem("META-INF/services/" + serviceProviderName));
        Set<String> availableProviderClasses = ServiceUtil.classNamesNamedIn(getClass().getClassLoader(),
                "META-INF/services/" + serviceProviderName);
        for (String providerClass : availableProviderClasses) {
            if (!RUNTIME_INITIALIZED_CLASSES.contains(providerClass)) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, providerClass));
            }
        }
    }

}
