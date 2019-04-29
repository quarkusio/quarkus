package io.quarkus.tika.deployment;

import java.util.HashSet;
import java.util.Set;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.resteasy.common.deployment.ResteasyJaxrsProviderBuildItem;

public class TikaProcessor {

    private static final Set<String> RUNTIME_INITIALIZED_CLASSES;
    static {
        RUNTIME_INITIALIZED_CLASSES = new HashSet<>();
    }

    @BuildStep
    void produceTikaJaxrsProvider(BuildProducer<ResteasyJaxrsProviderBuildItem> providers) {
        providers.produce(new ResteasyJaxrsProviderBuildItem("io.quarkus.tika.runtime.jaxrs.TikaMessageBodyReader"));
    }

    @BuildStep
    public void produceTikaCoreResources(BuildProducer<SubstrateResourceBuildItem> resource) throws Exception {
        resource.produce(new SubstrateResourceBuildItem("org/apache/tika/mime/tika-mimetypes.xml"));
        resource.produce(new SubstrateResourceBuildItem("org/apache/tika/parser/external/tika-external-parsers.xml"));
    }

    @BuildStep
    public void produceTikaParsersProviders(BuildProducer<SubstrateResourceBuildItem> resource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) throws Exception {
        produceTikaServiceProviders(resource, reflectiveClass, "org.apache.tika.parser.Parser");
        produceTikaServiceProviders(resource, reflectiveClass, "org.apache.tika.detect.Detector");
        produceTikaServiceProviders(resource, reflectiveClass, "org.apache.tika.detect.EncodingDetector");
    }

    @BuildStep
    public void produceRuntimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> resource) {
        for (String className : RUNTIME_INITIALIZED_CLASSES) {
            resource.produce(new RuntimeInitializedClassBuildItem(className));
        }
    }

    private void produceTikaServiceProviders(BuildProducer<SubstrateResourceBuildItem> resource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            String serviceProviderName) throws Exception {
        resource.produce(new SubstrateResourceBuildItem("META-INF/services/" + serviceProviderName));
        Set<String> availableProviderClasses = ServiceUtil.classNamesNamedIn(getClass().getClassLoader(),
                "META-INF/services/" + serviceProviderName);
        for (String providerClass : availableProviderClasses) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, providerClass));
        }
    }

}
