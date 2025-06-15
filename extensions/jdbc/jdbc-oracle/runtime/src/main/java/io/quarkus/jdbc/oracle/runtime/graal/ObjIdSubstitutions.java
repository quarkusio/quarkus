package io.quarkus.jdbc.oracle.runtime.graal;

import java.security.SecureRandom;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.InjectAccessors;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Substitutions required when `jdbc-oracle` is combined with `jdbc-db2`.
 */
@TargetClass(className = "java.rmi.server.ObjID")
public final class ObjIdSubstitutions {

    @Alias
    @InjectAccessors(SecureRandomAccessor.class)
    private static SecureRandom secureRandom;

}

class SecureRandomAccessor {
    private static volatile SecureRandom RANDOM;

    static SecureRandom get() {
        SecureRandom result = RANDOM;
        if (result == null) {
            /* Lazy initialization on first access. */
            result = initializeOnce();
        }
        return result;
    }

    private static synchronized SecureRandom initializeOnce() {
        SecureRandom result = RANDOM;
        if (result != null) {
            /* Double-checked locking is OK because INSTANCE is volatile. */
            return result;
        }

        result = new SecureRandom();
        RANDOM = result;
        return result;
    }
}
