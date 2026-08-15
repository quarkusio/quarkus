package io.quarkus.smallrye.jwt.build.runtime.graal;

import javax.crypto.SecretKey;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.impl.AuthenticatedCipherText;
import com.nimbusds.jose.util.Container;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * The Nimbus JOSE+JWT XChaCha20-Poly1305 (XC20P) content encryption is implemented with the optional Google Tink
 * dependency which SmallRye JWT does not require. These substitutions remove the references to the Tink classes
 * so that the native image can be built without it; XC20P is not supported by SmallRye JWT.
 */
@TargetClass(className = "com.nimbusds.jose.crypto.impl.XC20P")
final class Target_com_nimbusds_jose_crypto_impl_XC20P {

    @Substitute
    public static AuthenticatedCipherText encryptAuthenticated(SecretKey secretKey, Container<byte[]> ivContainer,
            byte[] plainText, byte[] authData) throws JOSEException {
        throw new JOSEException("XC20P content encryption is not supported");
    }

    @Substitute
    public static byte[] decryptAuthenticated(SecretKey secretKey, byte[] iv, byte[] cipherText, byte[] authData,
            byte[] authTag) throws JOSEException {
        throw new JOSEException("XC20P content encryption is not supported");
    }
}
