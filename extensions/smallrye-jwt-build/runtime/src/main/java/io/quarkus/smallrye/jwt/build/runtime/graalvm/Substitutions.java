package io.quarkus.smallrye.jwt.build.runtime.graalvm;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.jose4j.jwk.OctetKeyPairJsonWebKey", onlyWith = JavaVersionLessThan17.class)
final class Target_org_jose4j_jwk_OctetKeyPairJsonWebKey {
    @Substitute
    public Target_org_jose4j_jwk_OctetKeyPairJsonWebKey(java.security.PublicKey publicKey) {
    }

    @Substitute
    Target_org_jose4j_jwk_OctetKeyPairUtil subtypeKeyUtil() {
        return null;
    }
}

@TargetClass(className = "org.jose4j.keys.OctetKeyPairUtil", onlyWith = JavaVersionLessThan17.class)
final class Target_org_jose4j_jwk_OctetKeyPairUtil {
}

class JavaVersionLessThan17 implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        return Runtime.version().version().get(0) < 17;
    }
}
