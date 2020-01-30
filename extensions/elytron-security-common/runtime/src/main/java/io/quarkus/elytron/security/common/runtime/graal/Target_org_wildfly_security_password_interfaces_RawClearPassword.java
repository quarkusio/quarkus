package io.quarkus.elytron.security.common.runtime.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Replacement to allow access to the package private RawClearPassword class
 */
@TargetClass(className = "org.wildfly.security.password.interfaces.RawClearPassword")
final class Target_org_wildfly_security_password_interfaces_RawClearPassword {
    @Alias
    Target_org_wildfly_security_password_interfaces_RawClearPassword(final String algorithm, final char[] password) {
    }

}
