package io.quarkus.elytron.security.common.runtime.graal;

import java.util.Arrays;

import org.wildfly.common.Assert;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.wildfly.security.password.interfaces.BCryptPassword")
public final class Target_org_wildfly_security_password_interfaces_BCryptPassword {

    @Substitute
    static Target_org_wildfly_security_password_interfaces_BCryptPassword createRaw(String algorithm, byte[] hash, byte[] salt,
            int iterationCount) {
        Assert.checkNotNullParam("hash", hash);
        Assert.checkNotNullParam("salt", salt);
        Assert.checkNotNullParam("algorithm", algorithm);
        return (Target_org_wildfly_security_password_interfaces_BCryptPassword) (Object) new RawBCryptPassword(algorithm,
                Arrays.copyOf(hash, hash.length), Arrays.copyOf(salt, salt.length), iterationCount);
    }

    @TargetClass(className = "org.wildfly.security.password.interfaces.RawBCryptPassword")
    private static final class RawBCryptPassword {

        @Alias
        RawBCryptPassword(final String algorithm, final byte[] hash, final byte[] salt, final int iterationCount) {
        }
    }
}
