package org.jboss.shamrock.deployment.steps;

import java.util.List;
import java.util.Map;

import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.NativeImageSystemPropertyBuildItem;
import org.jboss.shamrock.deployment.builditem.ProxyDefinitionBuildItem;
import org.jboss.shamrock.deployment.builditem.ResourceBuildItem;
import org.jboss.shamrock.deployment.builditem.ResourceBundleBuildItem;
import org.jboss.shamrock.deployment.builditem.RuntimeInitializedClassBuildItem;
import org.jboss.shamrock.deployment.builditem.SubstrateConfigBuildItem;

//TODO: this should go away, once we decide on which one of the API's we want
class SubstrateConfigBuildStep {

    @BuildStep
    void build(List<SubstrateConfigBuildItem> substrateConfigBuildItems,
               BuildProducer<ProxyDefinitionBuildItem> proxy,
               BuildProducer<ResourceBundleBuildItem> resourceBundle,
               BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit,
               BuildProducer<NativeImageSystemPropertyBuildItem> nativeImage) {
        for (SubstrateConfigBuildItem substrateConfigBuildItem : substrateConfigBuildItems) {
            for (String i : substrateConfigBuildItem.getRuntimeInitializedClasses()) {
                runtimeInit.produce(new RuntimeInitializedClassBuildItem(i));
            }
            for (Map.Entry<String, String> e : substrateConfigBuildItem.getNativeImageSystemProperties().entrySet()) {
                nativeImage.produce(new NativeImageSystemPropertyBuildItem(e.getKey(), e.getValue()));
            }
            for (String i : substrateConfigBuildItem.getResourceBundles()) {
                resourceBundle.produce(new ResourceBundleBuildItem(i));
            }
            for (List<String> i : substrateConfigBuildItem.getProxyDefinitions()) {
                proxy.produce(new ProxyDefinitionBuildItem(i));
            }
        }
    }
}
