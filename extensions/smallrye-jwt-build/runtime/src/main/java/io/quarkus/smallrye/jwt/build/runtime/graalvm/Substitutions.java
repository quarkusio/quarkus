package io.quarkus.smallrye.jwt.build.runtime.graalvm;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.jose4j.jwk.OctetKeyPairJsonWebKey", onlyWith = JavaVersionLessThan17andOctetKeyPairOnClasspath.class)
final class Target_org_jose4j_jwk_OctetKeyPairJsonWebKey {
    @Substitute
    public Target_org_jose4j_jwk_OctetKeyPairJsonWebKey(java.security.PublicKey publicKey) {
        throw new UnsupportedOperationException(
                "OctetKeyPairJsonWebKey depends on EdECPrivateKeySpec which is not available in Java < 15");
    }

    @Substitute
    Target_org_jose4j_jwk_OctetKeyPairUtil subtypeKeyUtil() {
        throw new UnsupportedOperationException(
                "OctetKeyPairJsonWebKey depends on EdECPrivateKeySpec which is not available in Java < 15");
    }
}

@TargetClass(className = "org.jose4j.keys.OctetKeyPairUtil", onlyWith = JavaVersionLessThan17andOctetKeyPairOnClasspath.class)
final class Target_org_jose4j_jwk_OctetKeyPairUtil {
}

class JavaVersionLessThan17andOctetKeyPairOnClasspath implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        try {
            Class.forName("org.jose4j.jwk.OctetKeyPairJsonWebKey");
            Class.forName("org.jose4j.jwk.OctetKeyPairUtil");
            return Runtime.version().version().get(0) < 17;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
