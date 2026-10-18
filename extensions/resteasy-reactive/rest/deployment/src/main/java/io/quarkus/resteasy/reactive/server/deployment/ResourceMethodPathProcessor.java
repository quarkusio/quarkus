package io.quarkus.resteasy.reactive.server.deployment;

import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PATH;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.resteasy.reactive.server.runtime.ResourceMethodPathRecorder;

public class ResourceMethodPathProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void registerResourceMethodPaths(ResteasyReactiveResourceMethodEntriesBuildItem resourceMethodEntries,
            ShutdownContextBuildItem shutdownContext,
            ResourceMethodPathRecorder recorder) {
        Map<String, Map<String, String>> pathsByClass = new HashMap<>();
        // tracks the method already recorded for a (class, method name) pair, so that a method scanned more than once
        // is not mistaken for an overload
        Map<String, MethodInfo> selectedMethods = new HashMap<>();
        Set<String> ambiguousKeys = new HashSet<>();

        for (ResteasyReactiveResourceMethodEntriesBuildItem.Entry entry : resourceMethodEntries.getEntries()) {
            MethodInfo methodInfo = entry.getMethodInfo();
            AnnotationInstance pathAnnotation = methodInfo.annotation(PATH);
            if (pathAnnotation == null) {
                // reflection only selects methods with a method-level @Path, so ignore the rest
                continue;
            }
            String className = entry.getActualClassInfo().name().toString();
            String methodName = methodInfo.name();
            String key = className + "." + methodName;
            if (ambiguousKeys.contains(key)) {
                continue;
            }
            MethodInfo previous = selectedMethods.get(key);
            if (previous != null) {
                if (previous.equals(methodInfo)) {
                    // the very same method scanned again, not an overload
                    continue;
                }
                // more than one @Path method with the same simple name: omit the entry so that the reflective
                // fallback reproduces the original "Two methods with the same path" error
                ambiguousKeys.add(key);
                selectedMethods.remove(key);
                pathsByClass.get(className).remove(methodName);
                continue;
            }
            selectedMethods.put(key, methodInfo);
            pathsByClass.computeIfAbsent(className, k -> new HashMap<>()).put(methodName, pathAnnotation.value().asString());
        }
        pathsByClass.values().removeIf(Map::isEmpty);

        recorder.setResourceMethodPaths(pathsByClass);
        recorder.cleanUp(shutdownContext);
    }
}
