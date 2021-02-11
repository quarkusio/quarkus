package io.quarkus.jdbc.h2.runtime.graal;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.h2.tools.Server")
@Delete
public final class H2ServerDisable {
}
