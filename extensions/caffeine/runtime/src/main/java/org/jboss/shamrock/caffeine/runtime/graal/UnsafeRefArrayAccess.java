package io.quarkus.caffeine.runtime.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * @author Sanne Grinovero
 */
@TargetClass(className = "com.github.benmanes.caffeine.cache.UnsafeRefArrayAccess")
public final class UnsafeRefArrayAccess {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.ArrayIndexShift, declClass = Object[].class)
    public static int REF_ELEMENT_SHIFT;

}
