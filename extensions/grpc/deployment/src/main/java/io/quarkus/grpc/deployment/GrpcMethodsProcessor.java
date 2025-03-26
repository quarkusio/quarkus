package io.quarkus.grpc.deployment;

import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.execannotations.ExecutionModelAnnotationsAllowedBuildItem;

public class GrpcMethodsProcessor {
    @BuildStep
    ExecutionModelAnnotationsAllowedBuildItem grpcMethods(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        return new ExecutionModelAnnotationsAllowedBuildItem(new Predicate<MethodInfo>() {
            @Override
            public boolean test(MethodInfo method) {
                // either the method is on a `@GrpcService` class
                if (method.declaringClass().hasDeclaredAnnotation(GrpcDotNames.GRPC_SERVICE)) {
                    return true;
                }

                // or the method is inherited by a `@GrpcService` class
                for (ClassInfo subclass : index.getAllKnownSubclasses(method.declaringClass().name())) {
                    if (subclass.hasDeclaredAnnotation(GrpcDotNames.GRPC_SERVICE)) {
                        return true;
                    }
                }

                return false;
            }
        });
    }
}
