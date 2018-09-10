package org.hibernate.protean.substitutions.sizereduction;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * As a consequence of RemoveXSAttributeChecker, this needs to be removed too.
 */
@TargetClass(className = "com.sun.org.apache.xerces.internal.impl.xs.traversers.XSDAbstractTraverser")
@Delete
public final class RemoveXSDAbstractTraverser {
}
