package io.quarkus.security.runtime.graal;

import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;

import io.quarkus.security.runtime.SecurityProviderUtils;

final class BouncyCastlePackages {
    static final String ORG_BOUNCYCASTLE_CRYPTO_PACKAGE = "org.bouncycastle.crypto";
    static final String ORG_BOUNCYCASTLE_CRYPTO_FIPS_PACKAGE = "org.bouncycastle.crypto.fips";
    static final String ORG_BOUNCYCASTLE_CRYPTO_INTERNAL_PACKAGE = "org.bouncycastle.crypto.internal";
    static final String ORG_BOUNCYCASTLE_CRYPTO_GENERAL_PACKAGE = "org.bouncycastle.crypto.general";
    static final String ORG_BOUNCYCASTLE_OPENSSL_PACKAGE = "org.bouncycastle.openssl";
    static final Set<String> PACKAGES = Arrays.asList(Package.getPackages()).stream()
            .map(Package::getName)
            .filter(p -> p.startsWith(ORG_BOUNCYCASTLE_CRYPTO_PACKAGE) || p.startsWith(ORG_BOUNCYCASTLE_OPENSSL_PACKAGE))
            .collect(Collectors.toSet());
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

@com.oracle.svm.core.annotate.TargetClass(className = "org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider", onlyWith = BouncyCastleCryptoFips.class)
final class Target_org_bouncycastle_jcajce_provider_BouncyCastleFipsProvider {
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset) //
    private SecureRandom entropySource;
}

@com.oracle.svm.core.annotate.TargetClass(className = "org.bouncycastle.math.ec.ECPoint", onlyWith = BouncyCastleCryptoFips.class)
final class Target_org_bouncycastle_math_ec_ECPoint {
    @Alias //
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset) //
    private static SecureRandom testRandom;
}

// TODO: this should be removed when https://github.com/netty/netty/issues/14826 is addressed
// this substitution can be removed when io.quarkus.it.bouncycastle.BouncyCastleITCase#loadNettySslContext passes
@com.oracle.svm.core.annotate.TargetClass(className = "io.netty.handler.ssl.BouncyCastlePemReader", onlyWith = NettySslBountyCastleSupportRequired.class)
final class Target_io_netty_handler_ssl_BouncyCastlePemReader {
    @Alias
    private static volatile Provider bcProvider;
    @Alias
    private static volatile boolean attemptedLoading;
    @Alias
    private static volatile Throwable unavailabilityCause;

    @com.oracle.svm.core.annotate.Substitute
    public static boolean isAvailable() {
        if (!attemptedLoading) {
            // do what io.netty.handler.ssl.BouncyCastlePemReader.tryLoading does
            // however Netty creates a new provider instance that doesn't have all the services
            // while we take already created provider with all registered services
            bcProvider = Security.getProvider(SecurityProviderUtils.BOUNCYCASTLE_PROVIDER_NAME);
            if (bcProvider == null) {
                bcProvider = Security.getProvider(SecurityProviderUtils.BOUNCYCASTLE_FIPS_PROVIDER_NAME);
            }
            if (bcProvider == null) {
                tryLoading();
            } else {
                attemptedLoading = true;
            }
        }
        return unavailabilityCause == null;
    }

    @Alias
    private static void tryLoading() {

    }
}

class NettySslBountyCastleSupportRequired implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        // this package is used by the BouncyCastlePemReader and present in 'org.bouncycastle:bcpkix-jdk18on'
        if (BouncyCastlePackages.PACKAGES.contains(BouncyCastlePackages.ORG_BOUNCYCASTLE_OPENSSL_PACKAGE)) {
            try {
                Class.forName("io.netty.handler.ssl.BouncyCastlePemReader", false,
                        Thread.currentThread().getContextClassLoader());
                return true;
            } catch (Throwable e) {
                // class not available
            }
        }
        return false;
    }
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
