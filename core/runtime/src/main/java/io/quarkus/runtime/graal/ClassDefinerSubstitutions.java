package io.quarkus.runtime.graal;

import java.lang.invoke.MethodHandles;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

import io.smallrye.common.classloader.ClassDefiner;

final class ClassDefinerSubstitutions {
    @TargetClass(ClassDefiner.class)
    static final class Target_io_smallrye_common_classloader_ClassDefiner {
        @Delete
        public static Class<?> defineClass(MethodHandles.Lookup lookup, Class<?> parent, String className, byte[] classBytes) {
            return null;
        }
    }
}
