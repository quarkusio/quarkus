package org.infinispan.protean.substitutions;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * @author William Burns
 */
@TargetClass(className = "com.github.benmanes.caffeine.cache.UnsafeRefArrayAccess")
public final class CaffeineSubstitutions {

   @Alias
   @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.ArrayIndexShift, declClass = Object[].class)
   public static int REF_ELEMENT_SHIFT;

}
