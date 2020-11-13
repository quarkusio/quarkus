package io.quarkus.security.runtime;

import static io.quarkus.security.runtime.SecurityProviderUtils.addProvider;
import static io.quarkus.security.runtime.SecurityProviderUtils.findProviderIndex;
import static io.quarkus.security.runtime.SecurityProviderUtils.insertProvider;
import static io.quarkus.security.runtime.SecurityProviderUtils.loadProvider;
import static io.quarkus.security.runtime.SecurityProviderUtils.loadProviderWithParams;

import java.security.Provider;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class SecurityProviderRecorder {
    public void addBouncyCastleProvider(boolean inFipsMode) {
        final String providerName = inFipsMode ? SecurityProviderUtils.BOUNCYCASTLE_FIPS_PROVIDER_CLASS_NAME
                : SecurityProviderUtils.BOUNCYCASTLE_PROVIDER_CLASS_NAME;
        addProvider(loadProvider(providerName));
    }

    public void addBouncyCastleJsseProvider() {
        Provider bc = loadProvider(SecurityProviderUtils.BOUNCYCASTLE_PROVIDER_CLASS_NAME);
        Provider bcJsse = loadProvider(SecurityProviderUtils.BOUNCYCASTLE_JSSE_PROVIDER_CLASS_NAME);
        int sunJsseIndex = findProviderIndex(SecurityProviderUtils.SUN_JSSE_PROVIDER_NAME);
        insertProvider(bc, sunJsseIndex);
        insertProvider(bcJsse, sunJsseIndex + 1);
    }

    public void addBouncyCastleFipsJsseProvider() {
        Provider bc = loadProvider(SecurityProviderUtils.BOUNCYCASTLE_FIPS_PROVIDER_CLASS_NAME);
        int sunIndex = findProviderIndex(SecurityProviderUtils.SUN_PROVIDER_NAME);
        insertProvider(bc, sunIndex);
        Provider bcJsse = loadProviderWithParams(SecurityProviderUtils.SUN_JSSE_PROVIDER_CLASS_NAME,
                new Class[] { String.class }, new Object[] { SecurityProviderUtils.BOUNCYCASTLE_FIPS_PROVIDER_NAME });
        insertProvider(bcJsse, sunIndex + 1);
    }
}
