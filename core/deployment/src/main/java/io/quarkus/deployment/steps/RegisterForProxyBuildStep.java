package io.quarkus.deployment.steps;

import java.util.ArrayList;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.runtime.annotations.RegisterForProxy;

public class RegisterForProxyBuildStep {

    private static final DotName REGISTER_FOR_PROXY = DotName.createSimple(RegisterForProxy.class.getName());
    private static final DotName REGISTER_FOR_PROXY_LIST = DotName.createSimple(RegisterForProxy.List.class.getName());

    @BuildStep
    public void build(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxy) {
        var index = combinedIndexBuildItem.getIndex();
        var instances = new ArrayList<>(index.getAnnotations(REGISTER_FOR_PROXY));
        for (var list : index.getAnnotations(REGISTER_FOR_PROXY_LIST)) {
            for (var nested : list.value().asNestedArray()) {
                instances.add(AnnotationInstance.create(nested.name(), list.target(), nested.values()));
            }
        }
        for (var annotationInstance : instances) {
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