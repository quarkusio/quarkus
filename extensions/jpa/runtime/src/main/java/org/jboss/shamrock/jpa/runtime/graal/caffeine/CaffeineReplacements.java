package org.jboss.shamrock.jpa.runtime.graal.caffeine;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.github.benmanes.caffeine.cache.UnsafeRefArrayAccess")
public final class CaffeineReplacements {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.ArrayIndexShift, declClass = Object[].class)
    public static int REF_ELEMENT_SHIFT;

}
