package io.quarkus.runtime.graal;

import java.security.SecureRandom;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "sun.security.jca.JCAUtil")
public final class Target_sun_security_jca_JCAUtil {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private static SecureRandom def;
}
