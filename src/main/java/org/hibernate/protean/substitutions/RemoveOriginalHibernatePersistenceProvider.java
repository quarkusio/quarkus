package org.hibernate.protean.substitutions;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className="org.hibernate.jpa.HibernatePersistenceProvider")
@Delete
public final class RemoveOriginalHibernatePersistenceProvider {
}
