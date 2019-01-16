package org.jboss.shamrock.jpa.runtime.graal.service.jacc;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.hibernate.secure.internal.StandardJaccServiceImpl")
@Delete
public final class Delete_StandardJaccServiceImpl {
}
