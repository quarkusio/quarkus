package io.quarkus.smallrye.faulttolerance.runtime.graal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;

import org.graalvm.home.Version;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

public class FaultToleranceSubstitutions {
}

@TargetClass(className = "io.smallrye.faulttolerance.DefaultMethodFallbackProvider", onlyWith = GraalVM20OrEarlier.class)
final class Target_io_smallrye_faulttolerance_DefaultMethodFallbackProvider {

    @TargetClass(className = "io.smallrye.faulttolerance.ExecutionContextWithInvocationContext")
    static final class Target_io_smallrye_faulttolerance_ExecutionContextWithInvocationContext {

    }

    @Substitute
    static Object getFallback(Method fallbackMethod,
            Target_io_smallrye_faulttolerance_ExecutionContextWithInvocationContext ctx)
            throws IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException,
            Throwable {
        throw new RuntimeException("Not supported in native image when using GraalVM releases prior to 21.0.0");
    }

}

class GraalVM20OrEarlier implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        return Version.getCurrent().compareTo(21) < 0;
    }
}
