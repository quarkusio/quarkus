package io.quarkus.mutiny.runtime;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

class JCToolsSupport {
}

// Thanks to https://github.com/DataDog/dd-trace-java/pull/6020
@TargetClass(className = "org.jctools.util.UnsafeRefArrayAccess")
final class Target_org_jctools_util_UnsafeRefArrayAccess {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.ArrayIndexShift, declClass = Object[].class)
    public static int REF_ELEMENT_SHIFT;
}
