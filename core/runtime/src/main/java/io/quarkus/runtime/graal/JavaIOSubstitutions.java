package io.quarkus.runtime.graal;

import java.io.ObjectStreamClass;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(value = java.io.ObjectStreamClass.class, onlyWith = GraalVM20OrEarlier.class)
@SuppressWarnings({ "unused" })
final class Target_java_io_ObjectStreamClass {

    @Substitute
    private static ObjectStreamClass lookup(Class<?> cl, boolean all) {
        throw new UnsupportedOperationException("Serialization of class definitions not supported");
    }

    private Target_java_io_ObjectStreamClass(final Class<?> cl) {
        throw new UnsupportedOperationException("Serialization of class definitions not supported");
    }

    private Target_java_io_ObjectStreamClass() {
        throw new UnsupportedOperationException("Not supported");
    }

}
