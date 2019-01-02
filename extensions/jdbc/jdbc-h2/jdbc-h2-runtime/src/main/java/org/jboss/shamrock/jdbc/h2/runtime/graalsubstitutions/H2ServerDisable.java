package org.jboss.shamrock.jdbc.h2.runtime.graalsubstitutions;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.h2.tools.Server")
@Delete
public final class H2ServerDisable {
}
