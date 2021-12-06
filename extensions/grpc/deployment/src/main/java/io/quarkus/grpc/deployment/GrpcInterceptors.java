package io.quarkus.grpc.deployment;

import static io.quarkus.grpc.deployment.GrpcDotNames.GLOBAL_INTERCEPTOR;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

final class GrpcInterceptors {

    final Set<String> globalInterceptors;
    final Set<String> nonGlobalInterceptors;

    GrpcInterceptors(Set<String> globalInterceptors, Set<String> nonGlobalInterceptors) {
        this.globalInterceptors = globalInterceptors;
        this.nonGlobalInterceptors = nonGlobalInterceptors;
    }

    static GrpcInterceptors gatherInterceptors(IndexView index, DotName interceptorInterface) {
        Set<String> globalInterceptors = new HashSet<>();
        Set<String> nonGlobalInterceptors = new HashSet<>();

        Collection<ClassInfo> interceptorImplClasses = index.getAllKnownImplementors(interceptorInterface);
        for (ClassInfo interceptorImplClass : interceptorImplClasses) {
            if (Modifier.isAbstract(interceptorImplClass.flags())
                    || Modifier.isInterface(interceptorImplClass.flags())) {
                continue;
            }
            if (interceptorImplClass.classAnnotation(GLOBAL_INTERCEPTOR) == null) {
                nonGlobalInterceptors.add(interceptorImplClass.name().toString());
            } else {
                globalInterceptors.add(interceptorImplClass.name().toString());
            }
        }
        return new GrpcInterceptors(globalInterceptors, nonGlobalInterceptors);
    }

}
