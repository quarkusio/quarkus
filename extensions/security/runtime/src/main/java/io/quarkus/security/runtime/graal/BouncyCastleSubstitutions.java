package io.quarkus.security.runtime.graal;

import java.util.Arrays;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

final class BouncyCastlePackages {
    static final String ORG_BOUNCYCASTLE_CRYPTO_PACKAGE = "org.bouncycastle.crypto";
    static final String ORG_BOUNCYCASTLE_CRYPTO_FIPS_PACKAGE = "org.bouncycastle.crypto.fips";
    static final String ORG_BOUNCYCASTLE_CRYPTO_INTERNAL_PACKAGE = "org.bouncycastle.crypto.internal";
    static final String ORG_BOUNCYCASTLE_CRYPTO_GENERAL_PACKAGE = "org.bouncycastle.crypto.general";
    static final Set<String> PACKAGES = Arrays.asList(Package.getPackages()).stream()
            .map(p -> p.getName()).filter(p -> p.startsWith(ORG_BOUNCYCASTLE_CRYPTO_PACKAGE)).collect(Collectors.toSet());
}

@com.oracle.svm.core.annotate.TargetClass(className = "org.bouncycastle.crypto.general.DSA$1", onlyWith = BouncyCastleCryptoGeneral.class)
final class Target_org_bouncycastle_crypto_general_DSA$1 {
    @com.oracle.svm.core.annotate.Substitute
    public boolean hasTestPassed(Target_org_bouncycastle_crypto_internal_AsymmetricCipherKeyPair kp) {
        return true;
    }
}

@com.oracle.svm.core.annotate.TargetClass(className = "org.bouncycastle.crypto.general.DSTU4145$2", onlyWith = BouncyCastleCryptoGeneral.class)
final class Target_org_bouncycastle_crypto_general_DSTU4145$2 {
    @com.oracle.svm.core.annotate.Substitute
    public boolean hasTestPassed(Target_org_bouncycastle_crypto_internal_AsymmetricCipherKeyPair kp) {
        return true;
    }
}

@com.oracle.svm.core.annotate.TargetClass(className = "org.bouncycastle.crypto.general.ECGOST3410$2", onlyWith = BouncyCastleCryptoGeneral.class)
final class Target_org_bouncycastle_crypto_general_ECGOST3410$2 {
    @com.oracle.svm.core.annotate.Substitute
    public boolean hasTestPassed(Target_org_bouncycastle_crypto_internal_AsymmetricCipherKeyPair kp) {
        return true;
    }
}

@com.oracle.svm.core.annotate.TargetClass(className = "org.bouncycastle.crypto.general.GOST3410$1", onlyWith = BouncyCastleCryptoGeneral.class)
final class Target_org_bouncycastle_crypto_general_GOST3410$1 {
    @com.oracle.svm.core.annotate.Substitute
    public boolean hasTestPassed(Target_org_bouncycastle_crypto_internal_AsymmetricCipherKeyPair kp) {
        return true;
    }
}

@com.oracle.svm.core.annotate.TargetClass(className = "org.bouncycastle.crypto.fips.FipsDSA$2", onlyWith = BouncyCastleCryptoFips.class)
final class Target_org_bouncycastle_crypto_fips_FipsDSA$2 {
    @com.oracle.svm.core.annotate.Substitute
    public boolean hasTestPassed(Target_org_bouncycastle_crypto_internal_AsymmetricCipherKeyPair kp) {
        return true;
    }
}

@com.oracle.svm.core.annotate.TargetClass(className = "org.bouncycastle.crypto.fips.FipsEC$1", onlyWith = BouncyCastleCryptoFips.class)
final class Target_org_bouncycastle_crypto_fips_FipsEC$1 {
    @com.oracle.svm.core.annotate.Substitute
    public boolean hasTestPassed(Target_org_bouncycastle_crypto_internal_AsymmetricCipherKeyPair kp) {
        return true;
    }
}

@com.oracle.svm.core.annotate.TargetClass(className = "org.bouncycastle.crypto.fips.FipsRSA$3", onlyWith = BouncyCastleCryptoFips.class)
final class Target_org_bouncycastle_crypto_fips_FipsRSA$3 {
    @com.oracle.svm.core.annotate.Substitute
    public boolean hasTestPassed(Target_org_bouncycastle_crypto_internal_AsymmetricCipherKeyPair kp) {
        return true;
    }
}

@com.oracle.svm.core.annotate.TargetClass(className = "org.bouncycastle.crypto.fips.FipsRSA$EngineProvider$1", onlyWith = BouncyCastleCryptoFips.class)
final class Target_org_bouncycastle_crypto_fips_FipsRSA$EngineProvider$1 {
    @com.oracle.svm.core.annotate.Substitute
    public void evaluate(Target_org_bouncycastle_crypto_fips_RsaBlindedEngine rsaEngine) {
        // Complete
    }
}

@com.oracle.svm.core.annotate.TargetClass(className = "org.bouncycastle.crypto.internal.AsymmetricCipherKeyPair", onlyWith = BouncyCastleCryptoInternal.class)
final class Target_org_bouncycastle_crypto_internal_AsymmetricCipherKeyPair {
}

@com.oracle.svm.core.annotate.TargetClass(className = "org.bouncycastle.crypto.fips.RsaBlindedEngine", onlyWith = BouncyCastleCryptoFips.class)
final class Target_org_bouncycastle_crypto_fips_RsaBlindedEngine {
}

class BouncyCastleCryptoFips implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        return BouncyCastlePackages.PACKAGES.contains(BouncyCastlePackages.ORG_BOUNCYCASTLE_CRYPTO_FIPS_PACKAGE);
    }
}

class BouncyCastleCryptoGeneral implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        return BouncyCastlePackages.PACKAGES.contains(BouncyCastlePackages.ORG_BOUNCYCASTLE_CRYPTO_GENERAL_PACKAGE);
    }
}

class BouncyCastleCryptoInternal implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        return BouncyCastlePackages.PACKAGES.contains(BouncyCastlePackages.ORG_BOUNCYCASTLE_CRYPTO_INTERNAL_PACKAGE);
    }
}