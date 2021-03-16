package io.quarkus.jaeger.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.jaeger.runtime.JaegerDeploymentRecorder;

@TargetClass(className = "io.jaegertracing.internal.JaegerTracer")
public final class Target_JaegerTracer {

    @Substitute
    private static String getVersionFromProperties() {
        return JaegerDeploymentRecorder.jaegerVersion;
    }
}
