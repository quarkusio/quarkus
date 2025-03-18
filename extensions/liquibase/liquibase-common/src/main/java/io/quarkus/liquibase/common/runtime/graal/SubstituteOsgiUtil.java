package io.quarkus.liquibase.common.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(liquibase.util.OsgiUtil.class)
final class SubstituteOsgiUtil {

    @Substitute
    public static <T> Class<T> loadClass(String className) throws ClassNotFoundException {
        throw new UnsupportedOperationException("OSGi is not supported by quarkus-liquibase");
    }
}
