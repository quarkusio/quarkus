package io.quarkus.grpc.deployment;

import static io.quarkus.grpc.deployment.GrpcDotNames.GLOBAL_INTERCEPTOR;
import static org.jboss.jandex.AnnotationTarget.Kind.CLASS;
import static org.jboss.jandex.AnnotationTarget.Kind.METHOD;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

final class GrpcInterceptors {

    static final List<String> MICROMETER_INTERCEPTORS = List.of(
            "io.micrometer.core.instrument.binder.grpc.MetricCollectingClientInterceptor",
            "io.micrometer.core.instrument.binder.grpc.MetricCollectingServerInterceptor");

    final Set<String> globalInterceptors;
    final Set<String> nonGlobalInterceptors;

    GrpcInterceptors(Set<String> globalInterceptors, Set<String> nonGlobalInterceptors) {
        this.globalInterceptors = globalInterceptors;
        this.nonGlobalInterceptors = nonGlobalInterceptors;
    }

    static GrpcInterceptors gatherInterceptors(IndexView index, DotName interceptorInterface) {
        Set<DotName> allGlobalInterceptors = allGlobalInterceptors(index, interceptorInterface);
        Set<String> globalInterceptors = new HashSet<>();
        Set<String> nonGlobalInterceptors = new HashSet<>();

        Collection<ClassInfo> interceptorImplClasses = index.getAllKnownImplementors(interceptorInterface);
        for (ClassInfo interceptorImplClass : interceptorImplClasses) {
            if (Modifier.isAbstract(interceptorImplClass.flags())
                    || Modifier.isInterface(interceptorImplClass.flags())) {
                continue;
            }
            if (allGlobalInterceptors.contains(interceptorImplClass.name())) {
                globalInterceptors.add(interceptorImplClass.name().toString());
            } else {
                nonGlobalInterceptors.add(interceptorImplClass.name().toString());
            }
        }
        return new GrpcInterceptors(globalInterceptors, nonGlobalInterceptors);
    }

    private static Set<DotName> allGlobalInterceptors(IndexView index, DotName interceptorInterface) {
        Set<DotName> result = new HashSet<>();
        for (AnnotationInstance instance : index.getAnnotations(GLOBAL_INTERCEPTOR)) {
            DotName className = className(instance.target());
            if (isAssignableFrom(index, className, interceptorInterface)) {
                result.add(className);
            }
        }
        return result;
    }

    private static DotName className(AnnotationTarget target) {
        if (target.kind() == CLASS) {
            return target.asClass().name();
        } else if (target.kind() == METHOD) {
            return target.asMethod().returnType().name();
        }
        return null;
    }

    private static boolean isAssignableFrom(IndexView index, DotName className, DotName interceptorInterface) {
        if (className == null) {
            return false;
        }

        ClassInfo classInfo = index.getClassByName(className);
        List<DotName> interfaceNames = classInfo.interfaceNames();
        for (DotName in : interfaceNames) {
            if (in.equals(interceptorInterface)) {
                return true;
            }
            boolean result = isAssignableFrom(index, in, interceptorInterface);
            if (result) {
                return true;
            }
        }

        return isAssignableFrom(index, classInfo.superName(), interceptorInterface);
    }

}
