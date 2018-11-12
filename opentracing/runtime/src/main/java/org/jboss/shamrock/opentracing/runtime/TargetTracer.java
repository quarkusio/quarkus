package org.jboss.shamrock.opentracing.runtime;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
/**
 * Created by bob on 8/8/18.
 */
@TargetClass(className = "com.uber.jaeger.Tracer")
public final class TargetTracer {
    @Substitute
    private static String loadVersion() {
        return "Graal-0.2.1";
    }
}

