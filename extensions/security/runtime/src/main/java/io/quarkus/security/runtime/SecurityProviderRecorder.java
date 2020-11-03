package io.quarkus.security.runtime;

import java.security.Provider;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class SecurityProviderRecorder {
    public void addBouncyCastleProvider() {
        SecurityProviderUtils.addProvider(
                SecurityProviderUtils.loadProvider(SecurityProviderUtils.BOUNCYCASTLE_PROVIDER_CLASS_NAME));
    }

    public void addBouncyCastleJsseProvider() {
        Provider bc = SecurityProviderUtils.loadProvider(SecurityProviderUtils.BOUNCYCASTLE_PROVIDER_CLASS_NAME);
        Provider bcJsse = SecurityProviderUtils.loadProvider(SecurityProviderUtils.BOUNCYCASTLE_JSSE_PROVIDER_CLASS_NAME);
        int sunJsseIndex = SecurityProviderUtils.findSunJSSEProviderIndex();
        SecurityProviderUtils.insertProvider(bc, sunJsseIndex);
        SecurityProviderUtils.insertProvider(bcJsse, sunJsseIndex + 1);
    }
}
