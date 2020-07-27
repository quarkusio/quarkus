package io.quarkus.liquibase.runtime.graal;

import java.security.SecureRandom;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "liquibase.util.StringUtil")
final class SubstituteStringUtil {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private static SecureRandom rnd;

}
