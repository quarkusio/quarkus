package io.quarkus.smallrye.faulttolerance.deployment.substitutions.rxjava;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "rx.internal.util.unsafe.UnsafeAccess")
public final class UnsafeAccess {

    @Substitute
    public static boolean isUnsafeAvailable() {
        return false;
    }

}
