package io.quarkus.kubernetes.client.runtime.graal;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

class BouncyCastleSubstitutions {

    static class BouncyCastleMissing implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("org.bouncycastle.cert.X509v3CertificateBuilder");
                return false;
            } catch (ClassNotFoundException ignored) {
                return true;
            }
        }
    }

    @TargetClass(className = "io.netty.handler.ssl.util.SelfSignedCertificate", onlyWith = BouncyCastleMissing.class)
    public static final class Substitute_SelfSignedCertificate {

        @Substitute
        private static boolean isBouncyCastleAvailable() {
            return false;
        }

    }

    @TargetClass(className = "io.netty.handler.ssl.util.SelfSignedCertificate$Builder", onlyWith = BouncyCastleMissing.class)
    public static final class Substitute_SelfSignedCertificateBuilder {

        @Substitute
        boolean generateBc() {
            return false;
        }

    }

}
