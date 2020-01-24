package io.quarkus.elytron.security.common.runtime.graal;

import org.wildfly.common.Assert;
import org.wildfly.security.password.interfaces.MaskedPassword;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Replace the {@linkplain MaskedPassword} interface due to an issue with char[].clone() failures during native image
 * generation.
 */
@TargetClass(MaskedPassword.class)
final class Target_org_wildfly_security_password_interfaces_MaskedPassword {

    @Substitute
    static MaskedPassword createRaw(String algorithm, char[] initialKeyMaterial, int iterationCount, byte[] salt,
            byte[] maskedPasswordBytes, byte[] initializationVector) {
        Assert.checkNotNullParam("algorithm", algorithm);
        Assert.checkNotNullParam("initialKeyMaterial", initialKeyMaterial);
        Assert.checkNotNullParam("salt", salt);
        Assert.checkNotNullParam("maskedPasswordBytes", maskedPasswordBytes);

        char[] initialKeyMaterialClone = new char[initialKeyMaterial.length];
        System.arraycopy(initialKeyMaterial, 0, initialKeyMaterialClone, 0, initialKeyMaterial.length);

        byte[] saltClone = new byte[salt.length];
        System.arraycopy(salt, 0, saltClone, 0, salt.length);

        byte[] maskedPasswordBytesClone = new byte[maskedPasswordBytes.length];
        System.arraycopy(maskedPasswordBytes, 0, maskedPasswordBytesClone, 0, maskedPasswordBytes.length);

        return (MaskedPassword) (Object) new Target_org_wildfly_security_password_interfaces_RawMaskedPassword(algorithm,
                initialKeyMaterialClone, iterationCount, saltClone, maskedPasswordBytesClone, initializationVector);
    }

}
