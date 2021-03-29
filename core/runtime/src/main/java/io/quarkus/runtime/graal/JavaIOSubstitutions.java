package io.quarkus.runtime.graal;

import java.io.ObjectStreamClass;
import java.util.function.BooleanSupplier;

import org.graalvm.home.Version;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.runtime.graal.Target_java_io_ObjectStreamClass.GraalVM20OrEarlier;

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

    static class GraalVM20OrEarlier implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            return Version.getCurrent().compareTo(21) < 0;
        }
    }
}
