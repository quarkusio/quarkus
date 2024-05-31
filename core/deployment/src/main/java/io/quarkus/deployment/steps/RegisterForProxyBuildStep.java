package io.quarkus.deployment.steps;

import java.util.ArrayList;

import org.jboss.jandex.DotName;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.runtime.annotations.RegisterForProxy;

public class RegisterForProxyBuildStep {

    @BuildStep
    public void build(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxy) {
        for (var annotationInstance : combinedIndexBuildItem.getIndex()
                .getAnnotations(DotName.createSimple(RegisterForProxy.class.getName()))) {
            var targetsValue = annotationInstance.value("targets");
            var types = new ArrayList<String>();
            if (targetsValue == null) {
                var classInfo = annotationInstance.target().asClass();
                types.add(classInfo.name().toString());
                classInfo.interfaceNames().forEach(dotName -> types.add(dotName.toString()));
            } else {
                for (var type : targetsValue.asClassArray()) {
                    types.add(type.name().toString());
                }
            }
            proxy.produce(new NativeImageProxyDefinitionBuildItem(types));
        }
    }
}