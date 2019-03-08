package io.quarkus.elytron.security.runtime.graal;

import java.security.NoSuchAlgorithmException;
import java.security.Signature;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import sun.security.rsa.SunRsaSign;

/**
 * Override the {@linkplain Signature#getInstance(String)} to deal with creating the Signature by creating the
 * {@linkplain SunRsaSign} directly for "RSA" algorithm
 */
@TargetClass(Signature.class)
public final class Target_java_security_Signature {

    @Substitute
    public static Signature getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm.endsWith("RSA")) {
            SunRsaSign provider = new SunRsaSign();
            return Signature.getInstance(algorithm, provider);
        }
        throw new NoSuchAlgorithmException(algorithm);
    }
}
