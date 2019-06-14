package io.quarkus.jaeger.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "io.jaegertracing.internal.JaegerTracer")
public final class Target_JaegerTracer {

    @Substitute
    private static String loadVersion() {
        // TODO: Obtain Jaeger version
        return "";
    }
}
