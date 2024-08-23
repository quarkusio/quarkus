package io.quarkus.grpc.deployment;

import java.util.function.Predicate;

import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.execannotations.ExecutionModelAnnotationsAllowedBuildItem;

public class GrpcMethodsProcessor {
    @BuildStep
    ExecutionModelAnnotationsAllowedBuildItem grpcMethods() {
        return new ExecutionModelAnnotationsAllowedBuildItem(new Predicate<MethodInfo>() {
            @Override
            public boolean test(MethodInfo method) {
                return method.declaringClass().hasDeclaredAnnotation(GrpcDotNames.GRPC_SERVICE);
            }
        });
    }
}
