package io.quarkus.jdbc.mariadb.runtime.graal;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.mariadb.jdbc.internal.io.socket.SharedMemorySocket")
@Delete
public final class SharedMemorySocket_Removal {

}
