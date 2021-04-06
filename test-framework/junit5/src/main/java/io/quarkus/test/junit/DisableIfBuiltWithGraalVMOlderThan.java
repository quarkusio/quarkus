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
        GRAALVM_21_0(21, 0);

        private final int major;
        private final int minor;

        GraalVMVersion(int major, int minor) {
            this.major = major;
            this.minor = minor;
        }

        public int getMajor() {
            return major;
        }

        public int getMinor() {
            return minor;
        }

        /**
         * Compares this version with a tuple of major and minor parts representing another GraalVM version
         * 
         * @return {@code -1} if this version is older than the version represented by the supplied major and minor parts,
         *         {@code +1} if it's newer and {@code 0} if they represent the same version
         */
        public int compareTo(int major, int minor) {
            int majorComparison = Integer.compare(this.major, major);
            if (majorComparison != 0) {
                return majorComparison;
            }
            return Integer.compare(this.minor, minor);
        }

        @Override
        public String toString() {
            return "GraalVMVersion{" +
                    "major=" + major +
                    ", minor=" + minor +
                    '}';
        }
    }
}
