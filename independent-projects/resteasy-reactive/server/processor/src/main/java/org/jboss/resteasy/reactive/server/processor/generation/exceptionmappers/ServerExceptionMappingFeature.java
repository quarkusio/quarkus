package org.jboss.resteasy.reactive.server.processor.generation.exceptionmappers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.common.model.ResourceExceptionMapper;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.server.processor.ScannedApplication;
import org.jboss.resteasy.reactive.server.processor.scanning.FeatureScanner;
import org.jboss.resteasy.reactive.server.processor.util.GeneratedClassOutput;

public class ServerExceptionMappingFeature implements FeatureScanner {

    final Set<DotName> unwrappableTypes;
    final Set<String> additionalBeanAnnotations;

    /**
     * @param unwrappableTypes
     *        Types that can be unwrapped using
     *        {@link org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext#unwrap(Class)}
     * @param additionalBeanAnnotations
     *        Annotations that should be added to generated beans
     */
    public ServerExceptionMappingFeature(Set<DotName> unwrappableTypes, Set<String> additionalBeanAnnotations) {
        this.unwrappableTypes = unwrappableTypes;
        this.additionalBeanAnnotations = additionalBeanAnnotations;
    }

    @Override
    public FeatureScanResult integrate(IndexView index, ScannedApplication scannedApplication) {
        List<MethodInfo> methodExceptionMapper = scannedApplication.getResourceScanningResult()
                .getClassLevelExceptionMappers();
        GeneratedClassOutput classOutput = new GeneratedClassOutput();
        final Map<String, Map<String, String>> resultingMappers = new HashMap<>(methodExceptionMapper.size());
        for (MethodInfo methodInfo : methodExceptionMapper) {
            Map<String, String> generationResult = ServerExceptionMapperGenerator.generatePerClassMapper(methodInfo,
                    classOutput, unwrappableTypes, additionalBeanAnnotations);
            Map<String, String> classMappers;
            DotName classDotName = methodInfo.declaringClass().name();
            String name = classDotName.toString();
            if (resultingMappers.containsKey(name)) {
                classMappers = resultingMappers.get(name);
            } else {
                classMappers = new HashMap<>();
                resultingMappers.put(name, classMappers);
            }
            classMappers.putAll(generationResult);
        }
        for (ResourceClass entry : scannedApplication.getResourceClasses()) {
            if (resultingMappers.containsKey(entry.getClassName())) {
                entry.setClassLevelExceptionMappers(resultingMappers.get(entry.getClassName()));
            }
        }
        for (ResourceClass entry : scannedApplication.getSubResourceClasses()) {
            if (resultingMappers.containsKey(entry.getClassName())) {
                entry.setClassLevelExceptionMappers(resultingMappers.get(entry.getClassName()));
            }
        }

        for (AnnotationInstance instance : index.getAnnotations(ResteasyReactiveDotNames.SERVER_EXCEPTION_MAPPER)) {
            if (instance.target().kind() != AnnotationTarget.Kind.METHOD) {
                continue;
            }
            MethodInfo methodInfo = instance.target().asMethod();
            if (methodExceptionMapper.contains(methodInfo)) { // methods annotated with @ServerExceptionMapper that
                                                              // exist inside a Resource Class are handled differently
                continue;
            }
            // the user class itself is made to be a bean as we want the user to be able to declare dependencies
            // additionalBeans.addBeanClass(methodInfo.declaringClass().name().toString());
            Map<String, String> generatedClassNames = ServerExceptionMapperGenerator.generateGlobalMapper(methodInfo,
                    classOutput, unwrappableTypes, additionalBeanAnnotations, (m) -> false);
            for (Map.Entry<String, String> entry : generatedClassNames.entrySet()) {
                ResourceExceptionMapper<Throwable> mapper = new ResourceExceptionMapper<>()
                        .setClassName(entry.getValue());
                scannedApplication.getExceptionMappers().addExceptionMapper(entry.getKey(), mapper);
                AnnotationValue priorityValue = instance.value("priority");
                if (priorityValue != null) {
                    mapper.setPriority(priorityValue.asInt());
                }
            }
        }
        return new FeatureScanResult(classOutput.getOutput());
    }
}
