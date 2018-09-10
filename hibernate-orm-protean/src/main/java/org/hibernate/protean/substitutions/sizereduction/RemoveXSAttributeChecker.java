package org.hibernate.protean.substitutions.sizereduction;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * The internal Xerces implementation keeps state in static initializers:
 * get rid of it.
 */
@TargetClass(className = "com.sun.org.apache.xerces.internal.impl.xs.traversers.XSAttributeChecker")
@Delete
public final class RemoveXSAttributeChecker {
}
