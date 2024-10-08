package io.quarkus.smallrye.faulttolerance.deployment;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.microprofile.config.ConfigProvider;
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
import org.jboss.jandex.Type;

import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.AnnotationProxyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.gizmo.ClassOutput;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.faulttolerance.api.ApplyFaultTolerance;
import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;
import io.smallrye.faulttolerance.api.BeforeRetry;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.faulttolerance.api.CustomBackoff;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.faulttolerance.api.FibonacciBackoff;
import io.smallrye.faulttolerance.api.RateLimit;
import io.smallrye.faulttolerance.api.RetryWhen;
import io.smallrye.faulttolerance.autoconfig.FaultToleranceMethod;
import io.smallrye.faulttolerance.autoconfig.MethodDescriptor;

final class FaultToleranceScanner {
    private final IndexView index;
    private final AnnotationStore annotationStore;

    private final AnnotationProxyBuildItem proxy;
    private final ClassOutput output;

    private final RecorderContext recorderContext;

    private final BuildProducer<ReflectiveMethodBuildItem> reflectiveMethod;

    private final FaultToleranceMethodSearch methodSearch;

    FaultToleranceScanner(IndexView index, AnnotationStore annotationStore, AnnotationProxyBuildItem proxy,
            ClassOutput output, RecorderContext recorderContext, BuildProducer<ReflectiveMethodBuildItem> reflectiveMethod) {
        this.index = index;
        this.annotationStore = annotationStore;
        this.proxy = proxy;
        this.output = output;
        this.recorderContext = recorderContext;
        this.reflectiveMethod = reflectiveMethod;
        this.methodSearch = new FaultToleranceMethodSearch(index);
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
                // constructors and static initializers can't be intercepted
                continue;
            }
            if (method.isSynthetic()) {
                // synthetic methods can't be intercepted
                continue;
            }
            if (annotationStore.hasAnnotation(method, io.quarkus.arc.processor.DotNames.NO_CLASS_INTERCEPTORS)
                    && !annotationStore.hasAnyAnnotation(method, DotNames.FT_ANNOTATIONS)) {
                // methods annotated @NoClassInterceptors and not annotated with an interceptor binding are not intercepted
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

        result.beanClass = getClassProxy(beanClass);
        result.method = createMethodDescriptor(method);

        result.applyFaultTolerance = getAnnotation(ApplyFaultTolerance.class, method, beanClass, annotationsPresentDirectly);

        result.asynchronous = getAnnotation(Asynchronous.class, method, beanClass, annotationsPresentDirectly);
        result.asynchronousNonBlocking = getAnnotation(AsynchronousNonBlocking.class, method, beanClass,
                annotationsPresentDirectly);
        result.blocking = getAnnotation(Blocking.class, method, beanClass, annotationsPresentDirectly);
        result.nonBlocking = getAnnotation(NonBlocking.class, method, beanClass, annotationsPresentDirectly);

        result.bulkhead = getAnnotation(Bulkhead.class, method, beanClass, annotationsPresentDirectly);
        result.circuitBreaker = getAnnotation(CircuitBreaker.class, method, beanClass, annotationsPresentDirectly);
        result.circuitBreakerName = getAnnotation(CircuitBreakerName.class, method, beanClass, annotationsPresentDirectly);
        result.fallback = getAnnotation(Fallback.class, method, beanClass, annotationsPresentDirectly);
        result.rateLimit = getAnnotation(RateLimit.class, method, beanClass, annotationsPresentDirectly);
        result.retry = getAnnotation(Retry.class, method, beanClass, annotationsPresentDirectly);
        result.timeout = getAnnotation(Timeout.class, method, beanClass, annotationsPresentDirectly);

        result.customBackoff = getAnnotation(CustomBackoff.class, method, beanClass, annotationsPresentDirectly);
        result.exponentialBackoff = getAnnotation(ExponentialBackoff.class, method, beanClass, annotationsPresentDirectly);
        result.fibonacciBackoff = getAnnotation(FibonacciBackoff.class, method, beanClass, annotationsPresentDirectly);
        result.retryWhen = getAnnotation(RetryWhen.class, method, beanClass, annotationsPresentDirectly);
        result.beforeRetry = getAnnotation(BeforeRetry.class, method, beanClass, annotationsPresentDirectly);

        result.annotationsPresentDirectly = annotationsPresentDirectly;

        searchForMethods(result, beanClass, method, annotationsPresentDirectly);

        return result;
    }

    private MethodDescriptor createMethodDescriptor(MethodInfo method) {
        MethodDescriptor result = new MethodDescriptor();
        result.declaringClass = getClassProxy(method.declaringClass());
        result.name = method.name();
        result.parameterTypes = method.parameterTypes()
                .stream()
                .map(this::getClassProxy)
                .toArray(Class[]::new);
        result.returnType = getClassProxy(method.returnType());
        return result;
    }

    private <A extends Annotation> A getAnnotation(Class<A> annotationType, MethodInfo method,
            ClassInfo beanClass, Set<Class<? extends Annotation>> directlyPresent) {

        DotName annotationName = DotName.createSimple(annotationType);
        if (annotationStore.hasAnnotation(method, annotationName)) {
            directlyPresent.add(annotationType);
            AnnotationInstance annotation = annotationStore.getAnnotation(method, annotationName);
            return createAnnotation(annotationType, annotation);
        }

        return getAnnotationFromClass(annotationType, beanClass);
    }

    // ---

    private void searchForMethods(FaultToleranceMethod result, ClassInfo beanClass, MethodInfo method,
            Set<Class<? extends Annotation>> annotationsPresentDirectly) {
        if (result.fallback != null) {
            String fallbackMethod = getMethodNameFromConfig(method, annotationsPresentDirectly,
                    Fallback.class, "fallbackMethod");
            if (fallbackMethod == null) {
                fallbackMethod = result.fallback.fallbackMethod();
            }
            if (fallbackMethod != null && !fallbackMethod.isEmpty()) {
                ClassInfo declaringClass = method.declaringClass();
                Type[] parameterTypes = method.parameterTypes().toArray(new Type[0]);
                Type returnType = method.returnType();
                MethodInfo foundMethod = methodSearch.findFallbackMethod(beanClass,
                        declaringClass, fallbackMethod, parameterTypes, returnType);
                Set<MethodInfo> foundMethods = methodSearch.findFallbackMethodsWithExceptionParameter(beanClass,
                        declaringClass, fallbackMethod, parameterTypes, returnType);
                result.fallbackMethod = createMethodDescriptorIfNotNull(foundMethod);
                result.fallbackMethodsWithExceptionParameter = createMethodDescriptorsIfNotEmpty(foundMethods);
                if (foundMethod != null) {
                    reflectiveMethod.produce(new ReflectiveMethodBuildItem("@Fallback method", foundMethod));
                }
                for (MethodInfo m : foundMethods) {
                    reflectiveMethod.produce(new ReflectiveMethodBuildItem("@Fallback method", m));
                }
            }
        }

        if (result.beforeRetry != null) {
            String beforeRetryMethod = getMethodNameFromConfig(method, annotationsPresentDirectly,
                    BeforeRetry.class, "methodName");
            if (beforeRetryMethod == null) {
                beforeRetryMethod = result.beforeRetry.methodName();
            }
            if (beforeRetryMethod != null && !beforeRetryMethod.isEmpty()) {
                MethodInfo foundMethod = methodSearch.findBeforeRetryMethod(beanClass,
                        method.declaringClass(), beforeRetryMethod);
                result.beforeRetryMethod = createMethodDescriptorIfNotNull(foundMethod);
                if (foundMethod != null) {
                    reflectiveMethod.produce(new ReflectiveMethodBuildItem("@BeforeRetry method", foundMethod));
                }
            }
        }
    }

    // copy of generated code to obtain a config value and translation from reflection to Jandex
    // no need to check whether `ftAnnotation` is enabled, this will happen at runtime
    private String getMethodNameFromConfig(MethodInfo method, Set<Class<? extends Annotation>> annotationsPresentDirectly,
            Class<? extends Annotation> ftAnnotation, String memberName) {
        String result;
        org.eclipse.microprofile.config.Config config = ConfigProvider.getConfig();
        if (annotationsPresentDirectly.contains(ftAnnotation)) {
            // <classname>/<methodname>/<annotation>/<parameter>
            String key = method.declaringClass().name() + "/" + method.name() + "/" + ftAnnotation.getSimpleName() + "/"
                    + memberName;
            result = config.getOptionalValue(key, String.class).orElse(null);
        } else {
            // <classname>/<annotation>/<parameter>
            String key = method.declaringClass().name() + "/" + ftAnnotation.getSimpleName() + "/" + memberName;
            result = config.getOptionalValue(key, String.class).orElse(null);
        }
        if (result == null) {
            // <annotation>/<parameter>
            result = config.getOptionalValue(ftAnnotation.getSimpleName() + "/" + memberName, String.class).orElse(null);
        }
        return result;
    }

    private MethodDescriptor createMethodDescriptorIfNotNull(MethodInfo method) {
        return method == null ? null : createMethodDescriptor(method);
    }

    private List<MethodDescriptor> createMethodDescriptorsIfNotEmpty(Collection<MethodInfo> methods) {
        if (methods.isEmpty()) {
            return null;
        }
        List<MethodDescriptor> result = new ArrayList<>(methods.size());
        for (MethodInfo method : methods) {
            result.add(createMethodDescriptor(method));
        }
        return result;
    }

    // ---

    private <A extends Annotation> A getAnnotationFromClass(Class<A> annotationType, ClassInfo clazz) {
        DotName annotationName = DotName.createSimple(annotationType);
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

    // using class proxies instead of attempting to load the class, because the class
    // doesn't have to exist in the deployment classloader at all -- instead, it may be
    // generated by another Quarkus extension (such as RestClient Reactive)
    private Class<?> getClassProxy(ClassInfo clazz) {
        return recorderContext.classProxy(clazz.name().toString());
    }

    private Class<?> getClassProxy(Type type) {
        // Type.name() returns the right thing
        return recorderContext.classProxy(type.name().toString());
    }
}
