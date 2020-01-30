package io.quarkus.elytron.security.common.runtime.graal;

import java.util.Arrays;

import org.wildfly.common.Assert;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.wildfly.security.password.interfaces.BSDUnixDESCryptPassword")
public final class Target_org_wildfly_security_password_interfaces_BSDUnixDESCryptPassword {

    @Substitute
    static Target_org_wildfly_security_password_interfaces_BSDUnixDESCryptPassword createRaw(String algorithm, byte[] hash,
            int salt, int iterationCount) {
        Assert.checkNotNullParam("hash", hash);
        Assert.checkNotNullParam("algorithm", algorithm);
        return (Target_org_wildfly_security_password_interfaces_BSDUnixDESCryptPassword) (Object) new RawBSDUnixDESCryptPassword(
                algorithm, iterationCount, salt, Arrays.copyOf(hash, hash.length));
    }

    @TargetClass(className = "org.wildfly.security.password.interfaces.RawBSDUnixDESCryptPassword")
    private static final class RawBSDUnixDESCryptPassword {

        @Alias
        RawBSDUnixDESCryptPassword(final String algorithm, final int iterationCount, final int salt, final byte[] hash) {
        }
    }
}
