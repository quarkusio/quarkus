package io.quarkus.elytron.security.common.runtime.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Replacement to allow access to the package private RawMaskedPassword class
 */
@TargetClass(className = "org.wildfly.security.password.interfaces.RawMaskedPassword")
final class Target_org_wildfly_security_password_interfaces_RawMaskedPassword {

    @Alias
    Target_org_wildfly_security_password_interfaces_RawMaskedPassword(final String algorithm, final char[] initialKeyMaterial,
            final int iterationCount, final byte[] salt, final byte[] maskedPasswordBytes, final byte[] initializationVector) {
    }

}
