package io.quarkus.mtls.utils;

import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;

import io.quarkus.mtls.MutualTLSConfig;
import io.quarkus.mtls.MutualTLSProvider;

/**
 * Key manager implementation that pulls from a mutual TLS provider configuration and
 * refreshes automatically as certificates expire and are reissued.
 */
public class DynamicMutualTLSKeyManager extends X509ExtendedKeyManager {

    private static final String ID_ALIAS = "id";
    private static final char[] EMPTY_PASSWORD = "".toCharArray();

    private final MutualTLSProvider mutualTLSProvider;
    private final String mutualTLSProviderName;
    private MutualTLSConfig lastConfig;
    private X509ExtendedKeyManager keyManagerDelegate;

    public DynamicMutualTLSKeyManager(MutualTLSProvider mutualTLSProvider, String mutualTLSProviderName) {
        this.mutualTLSProvider = mutualTLSProvider;
        this.mutualTLSProviderName = mutualTLSProviderName;
    }

    private MutualTLSConfig getConfig() {
        return mutualTLSProvider.getConfig(mutualTLSProviderName);
    }

    private X509ExtendedKeyManager getDelegate() {
        MutualTLSConfig config = getConfig();
        if (config != lastConfig) {
            // Update cached trust manager
            try {
                KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                ks.load(null);
                ks.setKeyEntry(ID_ALIAS, config.getIdentityPrivateKey(), EMPTY_PASSWORD,
                        config.getIdentityCertificateChain().toArray(new X509Certificate[0]));

                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(ks, EMPTY_PASSWORD);

                for (KeyManager km : kmf.getKeyManagers()) {
                    if (km instanceof X509ExtendedKeyManager) {
                        keyManagerDelegate = (X509ExtendedKeyManager) km;
                        break;
                    }
                }
                if (keyManagerDelegate == null) {
                    throw new GeneralSecurityException("No X509ExtendedKeyManager found");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            lastConfig = config;
        }
        return keyManagerDelegate;
    }

    /// X509KeyManager

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return getDelegate().getClientAliases(keyType, issuers);
    }

    @Override
    public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
        return getDelegate().chooseClientAlias(keyTypes, issuers, socket);
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return getDelegate().getServerAliases(keyType, issuers);
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return getDelegate().chooseServerAlias(keyType, issuers, socket);
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return getDelegate().getCertificateChain(alias);
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        return getDelegate().getPrivateKey(alias);
    }

    /// X509ExtendedKeyManager

    @Override
    public String chooseEngineClientAlias(String[] keyTypes, Principal[] issuers, SSLEngine engine) {
        return getDelegate().chooseEngineClientAlias(keyTypes, issuers, engine);
    }

    @Override
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        return getDelegate().chooseEngineServerAlias(keyType, issuers, engine);
    }
}
