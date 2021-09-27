package io.quarkus.runtime.graal;

import java.security.SecureRandom;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.runtime.util.JavaVersionUtil;

@TargetClass(className = "sun.security.jca.JCAUtil", onlyWith = JavaVersionUtil.JDK17OrLater.class)
public final class Target_sun_security_jca_JCAUtil {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private static SecureRandom def;
}
