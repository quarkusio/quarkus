package io.quarkus.csrf.reactive.runtime.graal;

import java.security.SecureRandom;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;

@com.oracle.svm.core.annotate.TargetClass(className = "io.quarkus.csrf.reactive.runtime.CsrfHandler")
final class Target_io_quarkus_csrf_reactive_runtime_CsrfHandler {
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset) //
    private volatile SecureRandom secureRandom;

}
