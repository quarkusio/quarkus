package io.quarkus.security;

import static org.wildfly.common.Assert.checkNotNullParam;

import org.wildfly.security.password.interfaces.ClearPassword;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Replace the {@linkplain ClearPassword} interface due to an issue with char[].clone() failures during native image gen.
 * https://github.com/oracle/graal/issues/877
 */
@TargetClass(value = ClearPassword.class)
final class Target_org_wildfly_security_password_interfaces_ClearPassword {

    @Substitute
    static ClearPassword createRaw(String algorithm, char[] password) {
        checkNotNullParam("algorithm", algorithm);
        checkNotNullParam("password", password);
        char[] clone = new char[password.length];
        System.arraycopy(password, 0, clone, 0, password.length);

        // Cast the RawClearPassword replacement to ClearPassword and return it
        ClearPassword pass = (ClearPassword) (Object) new Target_org_wildfly_security_password_interfaces_RawClearPassword(
                algorithm, clone);
        return pass;
    }
}