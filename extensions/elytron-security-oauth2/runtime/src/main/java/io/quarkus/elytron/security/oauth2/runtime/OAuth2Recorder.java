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
import java.util.function.Supplier;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.wildfly.security.auth.realm.token.TokenSecurityRealm;
import org.wildfly.security.auth.realm.token.validator.OAuth2IntrospectValidator;
import org.wildfly.security.auth.server.SecurityRealm;
import org.wildfly.security.authz.Attributes;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.elytron.security.oauth2.runtime.auth.ElytronOAuth2CallerPrincipal;
import io.quarkus.elytron.security.oauth2.runtime.auth.OAuth2Augmentor;
import io.quarkus.elytron.security.oauth2.runtime.auth.OAuth2AuthMethodExtension;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.undertow.servlet.ServletExtension;

@Recorder
public class OAuth2Recorder {

    public RuntimeValue<SecurityRealm> createRealm(OAuth2Config config)
            throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException, KeyManagementException {
        OAuth2IntrospectValidator.Builder validatorBuilder = OAuth2IntrospectValidator.builder()
                .clientId(config.clientId)
                .clientSecret(config.clientSecret)
                .tokenIntrospectionUrl(URI.create(config.introspectionUrl).toURL());

        if (config.caCertFile.isPresent()) {
            validatorBuilder.useSslContext(createSSLContext(config));
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

    private SSLContext createSSLContext(OAuth2Config config)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        try (InputStream is = new FileInputStream(config.caCertFile.get())) {
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

    public Supplier<OAuth2Augmentor> augmentor(OAuth2Config config) {
        return new Supplier<OAuth2Augmentor>() {
            @Override
            public OAuth2Augmentor get() {
                return new OAuth2Augmentor(config.roleClaim);
            }
        };
    }

    /**
     * Create the JWTAuthMethodExtension servlet extension
     *
     * @param authMechanism - name to use for MP-JWT auth mechanism
     * @param container - bean container to create JWTAuthMethodExtension bean
     * @return JWTAuthMethodExtension
     */
    public ServletExtension createAuthExtension(String authMechanism, BeanContainer container) {
        OAuth2AuthMethodExtension authExt = container.instance(OAuth2AuthMethodExtension.class);
        authExt.setAuthMechanism(authMechanism);
        return authExt;
    }

}
