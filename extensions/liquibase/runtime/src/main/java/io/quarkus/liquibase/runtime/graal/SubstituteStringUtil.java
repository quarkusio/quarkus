package io.quarkus.liquibase.runtime.graal;

import java.security.SecureRandom;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.InjectAccessors;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "liquibase.util.StringUtil")
final class SubstituteStringUtil {

    @Alias
    @InjectAccessors(SecureRandomAccessors.class)
    private static SecureRandom rnd;

    public static final class SecureRandomAccessors {

        private static volatile SecureRandom volatileRandom;

        public static SecureRandom get() {
            SecureRandom localVolatileRandom = volatileRandom;
            if (localVolatileRandom == null) {
                synchronized (SecureRandomAccessors.class) {
                    localVolatileRandom = volatileRandom;
                    if (localVolatileRandom == null) {
                        volatileRandom = localVolatileRandom = new SecureRandom();
                    }
                }
            }
            return localVolatileRandom;
        }

        public static void set(SecureRandom rnd) {
            throw new IllegalStateException("The setter for liquibase.util.StringUtil#rnd shouldn't be called.");
        }
    }
}
