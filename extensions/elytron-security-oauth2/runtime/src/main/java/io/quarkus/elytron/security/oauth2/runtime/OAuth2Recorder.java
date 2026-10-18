package io.quarkus.elytron.security.oauth2.runtime;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.wildfly.security.auth.realm.token.TokenSecurityRealm;
import org.wildfly.security.auth.realm.token.validator.OAuth2IntrospectValidator;
import org.wildfly.security.auth.server.SecurityRealm;
import org.wildfly.security.authz.Attributes;

import io.quarkus.elytron.security.oauth2.runtime.auth.ElytronOAuth2CallerPrincipal;
import io.quarkus.elytron.security.oauth2.runtime.auth.OAuth2Augmentor;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;

@Recorder
public class OAuth2Recorder {
    private final RuntimeValue<OAuth2RuntimeConfig> runtimeConfig;

    public OAuth2Recorder(final RuntimeValue<OAuth2RuntimeConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public RuntimeValue<SecurityRealm> createRealm()
            throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException, KeyManagementException {
        OAuth2RuntimeConfig runtimeConfig = this.runtimeConfig.getValue();

        if (!runtimeConfig.clientId().isPresent() || !runtimeConfig.clientSecret().isPresent()
                || !runtimeConfig.introspectionUrl().isPresent()) {
            throw new ConfigurationException(
                    "client-id, client-secret and introspection-url must be configured when the oauth2 extension is enabled");
        }

        OAuth2IntrospectValidator.Builder validatorBuilder = OAuth2IntrospectValidator.builder()
                .clientId(runtimeConfig.clientId().get())
                .clientSecret(runtimeConfig.clientSecret().get())
                .tokenIntrospectionUrl(URI.create(runtimeConfig.introspectionUrl().get()).toURL());

        if (runtimeConfig.caCertFile().isPresent()) {
            validatorBuilder.useSslContext(createSSLContext(runtimeConfig));
        } else {
            validatorBuilder.useSslContext(SSLContext.getDefault());
        }

        if (runtimeConfig.connectionTimeout().isPresent()) {
            validatorBuilder.connectionTimeout((int) runtimeConfig.connectionTimeout().get().toMillis());
        }

        if (runtimeConfig.readTimeout().isPresent()) {
            validatorBuilder.readTimeout((int) runtimeConfig.readTimeout().get().toMillis());
        }

        OAuth2IntrospectValidator validator = validatorBuilder.build();

        TokenSecurityRealm tokenRealm = TokenSecurityRealm.builder()
                .validator(validator)
                .claimToPrincipal(claims -> new ElytronOAuth2CallerPrincipal(attributesToMap(claims)))
                .build();

        return new RuntimeValue<>(tokenRealm);
    }

    private Map<String, Object> attributesToMap(Attributes claims) {
        Map<String, Object> attributeMap = new HashMap<>();
        for (Attributes.Entry entry : claims.entries()) {
            if (entry.size() > 1) {
                attributeMap.put(entry.getKey(), entry.subList(0, entry.size()));
            } else {
                attributeMap.put(entry.getKey(), entry.get(0));
            }
        }
        return attributeMap;
    }

    private SSLContext createSSLContext(OAuth2RuntimeConfig runtimeConfig)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        try (InputStream is = new FileInputStream(runtimeConfig.caCertFile().get())) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate caCert = (X509Certificate) cf.generateCertificate(is);

            TrustManagerFactory tmf = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null); // You don't need the KeyStore instance to come from a file.
            ks.setCertificateEntry("caCert", caCert);

            tmf.init(ks);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            return sslContext;
        }
    }

    public RuntimeValue<OAuth2Augmentor> augmentor(OAuth2BuildTimeConfig buildTimeConfig) {
        return new RuntimeValue<>(new OAuth2Augmentor(buildTimeConfig.roleClaim()));
    }

}
