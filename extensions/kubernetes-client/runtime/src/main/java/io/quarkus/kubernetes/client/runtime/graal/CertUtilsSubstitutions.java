package io.quarkus.kubernetes.client.runtime.graal;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "io.fabric8.kubernetes.client.internal.CertUtils", onlyWith = NoBouncyCastleOpenSSL.class)
public final class CertUtilsSubstitutions {

    @Substitute
    static PrivateKey handleECKey(InputStream keyInputStream) throws IOException {
        throw new RuntimeException(
                "EC Keys are not supported when using the native binary, please add the org.bouncycastle:bcpkix-jdk18on dependency");
    }
}
