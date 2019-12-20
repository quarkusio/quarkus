package io.quarkus.smallrye.faulttolerance.runtime.graal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

public class FaultToleranceSubstitutions {
}

@TargetClass(className = "io.smallrye.faulttolerance.DefaultMethodFallbackProvider")
final class Target_io_smallrye_faulttolerance_DefaultMethodFallbackProvider {

    @TargetClass(className = "io.smallrye.faulttolerance.ExecutionContextWithInvocationContext")
    static final class Target_io_smallrye_faulttolerance_ExecutionContextWithInvocationContext {

    }

    @Substitute
    static Object getFallback(Method fallbackMethod,
            Target_io_smallrye_faulttolerance_ExecutionContextWithInvocationContext ctx)
            throws IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException,
            Throwable {
        throw new RuntimeException("Not implemented in native mode");
    }
}
