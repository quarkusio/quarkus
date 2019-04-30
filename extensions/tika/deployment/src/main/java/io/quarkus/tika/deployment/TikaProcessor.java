package io.quarkus.tika.deployment;

import java.util.HashSet;
import java.util.Set;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ServiceProviderBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.deployment.util.ServiceUtil;

public class TikaProcessor {

    private static final Set<String> RUNTIME_INITIALIZED_CLASSES;
    static {
        RUNTIME_INITIALIZED_CLASSES = new HashSet<>();
    }

    @BuildStep
    public void produceTikaCoreResources(BuildProducer<SubstrateResourceBuildItem> resource) throws Exception {
        resource.produce(new SubstrateResourceBuildItem("org/apache/tika/mime/tika-mimetypes.xml"));
        resource.produce(new SubstrateResourceBuildItem("org/apache/tika/parser/external/tika-external-parsers.xml"));
    }

    @BuildStep
    public void produceTikaParsersProviders(BuildProducer<ServiceProviderBuildItem> serviceProvider) throws Exception {
        produceTikaServiceProviders(serviceProvider, "org.apache.tika.parser.Parser");
        produceTikaServiceProviders(serviceProvider, "org.apache.tika.detect.Detector");
        produceTikaServiceProviders(serviceProvider, "org.apache.tika.detect.EncodingDetector");
    }

    @BuildStep
    public void produceRuntimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> resource) {
        for (String className : RUNTIME_INITIALIZED_CLASSES) {
            resource.produce(new RuntimeInitializedClassBuildItem(className));
        }
    }

    private void produceTikaServiceProviders(BuildProducer<ServiceProviderBuildItem> serviceProvider,
            String serviceProviderName) throws Exception {
        String serviceProviderPath = "META-INF/services/" + serviceProviderName;
        String[] availableProviderClasses = ServiceUtil.classNamesNamedIn(getClass().getClassLoader(),
                serviceProviderPath).toArray(new String[] {});
        serviceProvider.produce(new ServiceProviderBuildItem(serviceProviderPath, availableProviderClasses));
    }

}
