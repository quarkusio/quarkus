package io.quarkus.security.runtime;

import java.security.Provider;
import java.security.Security;

import io.quarkus.runtime.configuration.ConfigurationException;

public final class SecurityProviderUtils {
    public static final String SUN_JSSE_PROVIDER_NAME = "SunJSSE";
    public static final String BOUNCYCASTLE_PROVIDER_NAME = "BC";
    public static final String BOUNCYCASTLE_JSSE_PROVIDER_NAME = BOUNCYCASTLE_PROVIDER_NAME + "JSSE";
    public static final String BOUNCYCASTLE_PROVIDER_CLASS_NAME = "org.bouncycastle.jce.provider.BouncyCastleProvider";
    public static final String BOUNCYCASTLE_JSSE_PROVIDER_CLASS_NAME = "org.bouncycastle.jsse.provider.BouncyCastleJsseProvider";

    private SecurityProviderUtils() {

    }

    public static void addProvider(Provider provider) {
        try {
            Security.addProvider(provider);
        } catch (Exception t) {
            final String errorMessage = String.format("Security provider %s can not be added", provider.getName());
            throw new ConfigurationException(errorMessage, t);
        }
    }

    public static void insertProvider(Provider provider, int index) {
        try {
            Security.insertProviderAt(provider, index);
        } catch (Exception t) {
            final String errorMessage = String.format("Security provider %s can not be inserted", provider.getName());
            throw new ConfigurationException(errorMessage, t);
        }
    }

    public static Provider loadProvider(String providerClassName) {
        try {
            return (Provider) Thread.currentThread().getContextClassLoader().loadClass(providerClassName).newInstance();
        } catch (Exception t) {
            final String errorMessage = String.format("Security provider %s can not be registered", providerClassName);
            throw new ConfigurationException(errorMessage, t);
        }
    }

    public static int findSunJSSEProviderIndex() {
        Provider[] providers = Security.getProviders();
        for (int i = 0; i < providers.length; i++) {
            if (SUN_JSSE_PROVIDER_NAME.equals(providers[i].getName())) {
                return i + 1;
            }
        }
        return 1;
    }
}
