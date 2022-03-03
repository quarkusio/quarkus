package io.quarkus.test.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Used to signal that a test class or method should be disabled if the version of GraalVM used to build the native binary
 * under test was older than the supplied version.
 *
 * This annotation should only be used on a test classes annotated with {@link NativeImageTest} or
 * {@link QuarkusIntegrationTest}. If it is used on other test classes, it will have no effect.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisableIfBuiltWithGraalVMOlderThanCondition.class)
public @interface DisableIfBuiltWithGraalVMOlderThan {

    GraalVMVersion value();

    enum GraalVMVersion {
        GRAALVM_21_0(org.graalvm.home.Version.create(21, 0));

        private final org.graalvm.home.Version version;

        GraalVMVersion(org.graalvm.home.Version version) {
            this.version = version;
        }

        public org.graalvm.home.Version getVersion() {
            return version;
        }

        /**
         * Compares this version with another GraalVM version
         *
         * @return {@code -1} if this version is older than the other version,
         *         {@code +1} if it's newer and {@code 0} if they represent the same version
         */
        public int compareTo(org.graalvm.home.Version version) {
            return this.version.compareTo(version);
        }

        @Override
        public String toString() {
            return "GraalVMVersion{" +
                    "version=" + version.toString() +
                    '}';
        }
    }
}
