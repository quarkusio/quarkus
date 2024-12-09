package io.quarkus.smallrye.openapi.deployment.filter;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

public record ClassAndMethod(ClassInfo classInfo, MethodInfo method) {

}
