package io.quarkus.qrs.deployment.processor;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.qrs.deployment.framework.EndpointIndexer;
import io.quarkus.qrs.deployment.framework.QrsDotNames;
import io.quarkus.qrs.runtime.QrsRecorder;
import io.quarkus.qrs.runtime.model.ResourceClass;
import io.quarkus.vertx.http.deployment.FilterBuildItem;

public class QrsProcessor {

    @BuildStep
    public FeatureBuildItem buildSetup() {
        return new FeatureBuildItem("qrs");
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public FilterBuildItem setupEndpoints(BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            BeanContainerBuildItem beanContainerBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            QrsRecorder recorder, ExecutorBuildItem executorBuildItem) {
        Collection<AnnotationInstance> paths = beanArchiveIndexBuildItem.getIndex().getAnnotations(QrsDotNames.PATH);

        Collection<AnnotationInstance> allPaths = new ArrayList<>(paths);

        if (allPaths.isEmpty()) {
            // no detected @Path, bail out
            return null;
        }

        Map<DotName, ClassInfo> scannedResources = new HashMap<>();
        Set<DotName> pathInterfaces = new HashSet<>();
        for (AnnotationInstance annotation : allPaths) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                ClassInfo clazz = annotation.target().asClass();
                if (!Modifier.isInterface(clazz.flags())) {
                    scannedResources.put(clazz.name(), clazz);
                } else {
                    pathInterfaces.add(clazz.name());
                }
            }
        }

        List<ResourceClass> resourceClasses = new ArrayList<>();
        for (ClassInfo i : scannedResources.values()) {
            resourceClasses.add(EndpointIndexer.createEndpoints(beanArchiveIndexBuildItem.getIndex(), i,
                    beanContainerBuildItem.getValue(), generatedClassBuildItemBuildProducer, recorder));
        }

        return new FilterBuildItem(recorder.handler(resourceClasses, executorBuildItem.getExecutorProxy()), 10);
    }

}
