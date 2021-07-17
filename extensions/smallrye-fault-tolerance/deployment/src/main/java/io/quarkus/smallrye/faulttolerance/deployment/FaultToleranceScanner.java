package io.quarkus.smallrye.faulttolerance.deployment;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.deployment.builditem.AnnotationProxyBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.gizmo.ClassOutput;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.faulttolerance.api.CustomBackoff;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.faulttolerance.api.FibonacciBackoff;
import io.smallrye.faulttolerance.autoconfig.FaultToleranceMethod;
import io.smallrye.faulttolerance.autoconfig.MethodDescriptor;

final class FaultToleranceScanner {
    private final IndexView index;
    private final AnnotationStore annotationStore;

    private final AnnotationProxyBuildItem proxy;
    private final ClassOutput output;

    FaultToleranceScanner(IndexView index, AnnotationStore annotationStore, AnnotationProxyBuildItem proxy,
            ClassOutput output) {
        this.index = index;
        this.annotationStore = annotationStore;
        this.proxy = proxy;
        this.output = output;
    }

    boolean hasFTAnnotations(ClassInfo clazz) {
        // first check annotations on type
        if (annotationStore.hasAnyAnnotation(clazz, DotNames.FT_ANNOTATIONS)) {
            return true;
        }

        // then check on the methods
        for (MethodInfo method : clazz.methods()) {
            if (annotationStore.hasAnyAnnotation(method, DotNames.FT_ANNOTATIONS)) {
                return true;
            }
        }

        // then check on the parent
        DotName parentClassName = clazz.superName();
        if (parentClassName == null || parentClassName.equals(DotNames.OBJECT)) {
            return false;
        }
        ClassInfo parentClass = index.getClassByName(parentClassName);
        if (parentClass == null) {
            return false;
        }
        return hasFTAnnotations(parentClass);
    }

    void forEachMethod(ClassInfo clazz, Consumer<MethodInfo> action) {
        for (MethodInfo method : clazz.methods()) {
            if (method.name().startsWith("<")) {
                // constructors (or static init blocks) can't be intercepted
                continue;
            }
            if (method.isSynthetic()) {
                // synthetic methods can't be intercepted
                continue;
            }

            action.accept(method);
        }

        DotName parentClassName = clazz.superName();
        if (parentClassName == null || parentClassName.equals(DotNames.OBJECT)) {
            return;
        }
        ClassInfo parentClass = index.getClassByName(parentClassName);
        if (parentClass == null) {
            return;
        }
        forEachMethod(parentClass, action);
    }

    FaultToleranceMethod createFaultToleranceMethod(ClassInfo beanClass, MethodInfo method) {
        Set<Class<? extends Annotation>> annotationsPresentDirectly = new HashSet<>();

        FaultToleranceMethod result = new FaultToleranceMethod();

        result.beanClass = load(beanClass.name());
        result.method = createMethodDescriptor(method);

        result.asynchronous = getAnnotation(Asynchronous.class, method, beanClass, annotationsPresentDirectly);
        result.bulkhead = getAnnotation(Bulkhead.class, method, beanClass, annotationsPresentDirectly);
        result.circuitBreaker = getAnnotation(CircuitBreaker.class, method, beanClass, annotationsPresentDirectly);
        result.fallback = getAnnotation(Fallback.class, method, beanClass, annotationsPresentDirectly);
        result.retry = getAnnotation(Retry.class, method, beanClass, annotationsPresentDirectly);
        result.timeout = getAnnotation(Timeout.class, method, beanClass, annotationsPresentDirectly);

        result.circuitBreakerName = getAnnotation(CircuitBreakerName.class, method, beanClass, annotationsPresentDirectly);
        result.customBackoff = getAnnotation(CustomBackoff.class, method, beanClass, annotationsPresentDirectly);
        result.exponentialBackoff = getAnnotation(ExponentialBackoff.class, method, beanClass, annotationsPresentDirectly);
        result.fibonacciBackoff = getAnnotation(FibonacciBackoff.class, method, beanClass, annotationsPresentDirectly);

        result.blocking = getAnnotation(Blocking.class, method, beanClass, annotationsPresentDirectly);
        result.nonBlocking = getAnnotation(NonBlocking.class, method, beanClass, annotationsPresentDirectly);

        result.annotationsPresentDirectly = annotationsPresentDirectly;

        return result;
    }

    private MethodDescriptor createMethodDescriptor(MethodInfo method) {
        MethodDescriptor result = new MethodDescriptor();
        result.declaringClass = load(method.declaringClass().name());
        result.name = method.name();
        result.parameterTypes = method.parameters()
                .stream()
                .map(JandexUtil::loadRawType)
                .toArray(Class[]::new);
        result.returnType = JandexUtil.loadRawType(method.returnType());
        return result;
    }

    private <A extends Annotation> A getAnnotation(Class<A> annotationType, MethodInfo method,
            ClassInfo beanClass, Set<Class<? extends Annotation>> directlyPresent) {

        DotName annotationName = DotName.createSimple(annotationType.getName());
        if (annotationStore.hasAnnotation(method, annotationName)) {
            directlyPresent.add(annotationType);
            AnnotationInstance annotation = annotationStore.getAnnotation(method, annotationName);
            return createAnnotation(annotationType, annotation);
        }

        return getAnnotationFromClass(annotationType, beanClass);
    }

    private <A extends Annotation> A getAnnotationFromClass(Class<A> annotationType, ClassInfo clazz) {
        DotName annotationName = DotName.createSimple(annotationType.getName());
        if (annotationStore.hasAnnotation(clazz, annotationName)) {
            AnnotationInstance annotation = annotationStore.getAnnotation(clazz, annotationName);
            return createAnnotation(annotationType, annotation);
        }

        // then check on the parent
        DotName parentClassName = clazz.superName();
        if (parentClassName == null || parentClassName.equals(DotNames.OBJECT)) {
            return null;
        }
        ClassInfo parentClass = index.getClassByName(parentClassName);
        if (parentClass == null) {
            return null;
        }
        return getAnnotationFromClass(annotationType, parentClass);
    }

    private <A extends Annotation> A createAnnotation(Class<A> annotationType, AnnotationInstance instance) {
        return proxy.builder(instance, annotationType).build(output);
    }

    private static Class<?> load(DotName name) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(name.toString());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
