package io.quarkus.jdbc.postgresql.runtime.graal;

import org.postgresql.sspi.NTDSAPIWrapper;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(NTDSAPIWrapper.class)
@Delete
public final class Remove_NTDSAPIWrapper {

}
