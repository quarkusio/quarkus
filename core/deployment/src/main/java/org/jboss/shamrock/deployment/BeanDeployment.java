package org.jboss.shamrock.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;

public class BeanDeployment {

    private final List<String> additionalBeans = new ArrayList<>();

    private final Map<String, byte[]> generatedBeans = new HashMap<>();

    // Lite profile
    private final List<BiFunction<AnnotationTarget, Collection<AnnotationInstance>, Collection<AnnotationInstance>>> annotationTransformers = new ArrayList<>();

    private final List<DotName> resourceAnnotations = new ArrayList<>();

    // Full profile
    private final List<String> extensions = new ArrayList<>();

    public void addAdditionalBean(Class<?>... beanClass) {
        additionalBeans.addAll(Arrays.stream(beanClass).map(Class::getName).collect(Collectors.toList()));
    }

    public void addAdditionalBean(String... beanClass) {
        additionalBeans.addAll(Arrays.stream(beanClass).collect(Collectors.toList()));
    }

    public void addGeneratedBean(String name, byte[] bean) {
        generatedBeans.put(name, bean);
    }

    public void addAnnotationTransformer(BiFunction<AnnotationTarget, Collection<AnnotationInstance>, Collection<AnnotationInstance>> transformer) {
        annotationTransformers.add(transformer);
    }

    public void addExtension(String extensionClass) {
        extensions.add(extensionClass);
    }

    public void addResourceAnnotation(DotName resourceAnnotation) {
        resourceAnnotations.add(resourceAnnotation);
    }

    public List<String> getAdditionalBeans() {
        return additionalBeans;
    }

    public Map<String, byte[]> getGeneratedBeans() {
        return generatedBeans;
    }

    public List<BiFunction<AnnotationTarget, Collection<AnnotationInstance>, Collection<AnnotationInstance>>> getAnnotationTransformers() {
        return annotationTransformers;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public List<DotName> getResourceAnnotations() {
        return resourceAnnotations;
    }

}
