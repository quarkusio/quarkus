package io.quarkus.it.hibernate.multitenancy;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import io.agroal.pool.ConnectionHandler;

public class AgroalRuntimeDataSourceCreationFeature implements Feature {
    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        RuntimeReflection.register(ConnectionHandler[].class);
    }
}
