package io.quarkus.qrs.deployment.framework;

import static io.quarkus.qrs.deployment.framework.QrsDotNames.CONSUMES;
import static io.quarkus.qrs.deployment.framework.QrsDotNames.PATH;
import static io.quarkus.qrs.deployment.framework.QrsDotNames.PRODUCES;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.arc.processor.DotNames;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.qrs.runtime.QrsRecorder;
import io.quarkus.qrs.runtime.model.ResourceClass;
import io.quarkus.qrs.runtime.model.ResourceMethod;
import io.quarkus.qrs.runtime.spi.EndpointInvoker;

public class EndpointIndexer {

    public static ResourceClass createEndpoints(IndexView index, ClassInfo classInfo, BeanContainer beanContainer,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer, QrsRecorder recorder) {
        List<ResourceMethod> methods = createEndpoints(index, classInfo, classInfo, new HashSet<>(),
                generatedClassBuildItemBuildProducer, recorder);
        ResourceClass clazz = new ResourceClass();
        clazz.getMethods().addAll(methods);
        clazz.setClassName(classInfo.name().toString());
        AnnotationInstance pathAnnotation = classInfo.classAnnotation(PATH);
        if (pathAnnotation != null) {
            String path = pathAnnotation.value().asString();
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            clazz.setPath(path);
        }
        clazz.setFactory(recorder.factory(clazz.getClassName(), beanContainer));
        return clazz;
    }

    private static List<ResourceMethod> createEndpoints(IndexView index, ClassInfo currentClassInfo,
            ClassInfo actualEndpointInfo, Set<String> seenMethods,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer, QrsRecorder recorder) {
        List<ResourceMethod> ret = new ArrayList<>();
        String classProduces = readStringValue(currentClassInfo.classAnnotation(QrsDotNames.PRODUCES));
        String classConsumes = readStringValue(currentClassInfo.classAnnotation(QrsDotNames.CONSUMES));
        AnnotationInstance pathAnnotation = actualEndpointInfo.classAnnotation(PATH);

        for (DotName httpMethod : QrsDotNames.JAXRS_METHOD_ANNOTATIONS) {
            List<AnnotationInstance> foundMethods = currentClassInfo.annotations().get(httpMethod);
            if (foundMethods != null) {
                for (AnnotationInstance annotation : foundMethods) {
                    MethodInfo info = annotation.target().asMethod();
                    String descriptor = AsmUtil.getDescriptor(info, s -> null);
                    if (seenMethods.contains(descriptor)) {
                        continue;
                    }
                    seenMethods.add(descriptor);
                    String methodPath = readStringValue(info.annotation(PATH));
                    if (methodPath != null) {
                        if (!methodPath.startsWith("/")) {
                            methodPath = "/" + methodPath;
                        }
                    }
                    if (pathAnnotation != null) {
                        methodPath = appendPath(pathAnnotation.value().asString(), methodPath);
                    }
                    String produces = readStringValue(info.annotation(PRODUCES), classProduces);
                    String consumes = readStringValue(info.annotation(CONSUMES), classConsumes);
                    ResourceMethod method = new ResourceMethod()
                            .setMethod(annotationToMethod(httpMethod))
                            .setPath(methodPath)
                            .setConsumes(consumes)
                            .setName(info.name())
                            .setParameters(info.parameters().stream().map(s -> s.asClassType().toString()).toArray(String[]::new))
                            .setReturnType(info.returnType().asClassType().toString())
                            .setProduces(produces);

                    StringBuilder sigBuilder = new StringBuilder();
                    sigBuilder.append(method.getName())
                            .append(method.getReturnType());
                    for (String t : method.getParameters()) {
                        sigBuilder.append(t);
                    }
                    String baseName = currentClassInfo.name() + "$qrsinvoker$" + method.getName() + "_"
                            + HashUtil.sha1(sigBuilder.toString());
                    try (ClassCreator classCreator = new ClassCreator(
                            new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true), baseName, null,
                            Object.class.getName(), EndpointInvoker.class.getName())) {
                        MethodCreator mc = classCreator.getMethodCreator("invoke", Object.class, Object.class, Object[].class);
                        ResultHandle[] args = new ResultHandle[method.getParameters().length];
                        ResultHandle array = mc.getMethodParam(1);
                        for (int i = 0; i < method.getParameters().length; ++i) {
                            args[i] = mc.readArrayValue(array, i);
                        }
                        ResultHandle res = mc.invokeVirtualMethod(info, mc.getMethodParam(0), args);
                        mc.returnValue(res);
                    }
                    method.setInvoker(recorder.invoker(baseName));

                    ret.add(method);
                }
            }
        }
        DotName superClassName = currentClassInfo.superName();
        if ( superClassName != null && !superClassName.equals(DotNames.OBJECT)) {
            ClassInfo superClass = index.getClassByName(superClassName);
            ret.addAll(createEndpoints(index, superClass, actualEndpointInfo, seenMethods,
                    generatedClassBuildItemBuildProducer, recorder));
        }
        return ret;
    }

    private static String annotationToMethod(DotName httpMethod) {
        if (httpMethod.equals(QrsDotNames.GET)) {
            return "GET";
        } else if (httpMethod.equals(QrsDotNames.POST)) {
            return "POST";
        } else if (httpMethod.equals(QrsDotNames.HEAD)) {
            return "HEAD";
        } else if (httpMethod.equals(QrsDotNames.PUT)) {
            return "PUT";
        } else if (httpMethod.equals(QrsDotNames.DELETE)) {
            return "DELETE";
        } else if (httpMethod.equals(QrsDotNames.PATCH)) {
            return "PATCH";
        } else if (httpMethod.equals(QrsDotNames.OPTIONS)) {
            return "OPTIONS";
        }
        throw new IllegalStateException("Unknown HTTP method annotation " + httpMethod);
    }

    public static String readStringValue(AnnotationInstance annotation, String defaultValue) {
        String val = readStringValue(annotation);
        return val == null ? defaultValue : val;
    }

    public static String readStringValue(AnnotationInstance Annotation) {
        String classProduces = null;
        if (Annotation != null) {
            classProduces = Annotation.value().asString();
        }
        return classProduces;
    }

    private static String appendPath(String prefix, String suffix) {
        if (prefix == null) {
            return suffix;
        } else if (suffix == null) {
            return prefix;
        }
        if ((prefix.endsWith("/") && !suffix.startsWith("/")) ||
                (!prefix.endsWith("/") && suffix.startsWith("/"))) {
            return prefix + suffix;
        } else if (prefix.endsWith("/")) {
            return prefix.substring(0, prefix.length() - 1) + suffix;
        } else {
            return prefix + "/" + suffix;
        }
    }

}
