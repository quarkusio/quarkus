package io.quarkus.tls.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;

import io.quarkus.tls.KeyStoreAndKeyCertOptions;
import io.quarkus.tls.runtime.config.PqcEnforcementPolicy;
import io.quarkus.tls.runtime.config.TlsBucketConfig;
import io.smallrye.config.ConfigInstanceBuilder;
import io.vertx.core.net.KeyStoreOptions;

/**
 * The content snapshot must detect a re-issued intermediate when the leaf and the options are unchanged.
 */
class VertxCertificateHolderContentTest {

    // Same options whatever the store content, like a PKCS#11 token or a custom KeyStoreFactory
    private static final KeyStoreOptions OPTIONS = new KeyStoreOptions().setType("PKCS12").setPassword("password");

    @Test
    void reissuedIntermediateIsAContentChange() throws Exception {
        KeyPair leafKeys = rsa();
        KeyPair intermediateKeysA = rsa();
        KeyPair intermediateKeysB = rsa();
        X509Certificate intermediateA = certificate("CN=intermediate", "CN=root", intermediateKeysA,
                intermediateKeysA.getPrivate());
        X509Certificate intermediateB = certificate("CN=intermediate", "CN=root", intermediateKeysB,
                intermediateKeysB.getPrivate());
        X509Certificate leaf = certificate("CN=leaf", "CN=intermediate", leafKeys, intermediateKeysA.getPrivate());

        List<Object> before = holder(store(leafKeys, leaf, intermediateA)).content();

        assertThat(holder(store(leafKeys, leaf, intermediateA)).content()).isEqualTo(before);
        assertThat(holder(store(leafKeys, leaf, intermediateB)).content()).isNotEqualTo(before);
    }

    private static VertxCertificateHolder holder(KeyStore store) {
        TlsBucketConfig config = ConfigInstanceBuilder.forInterface(TlsBucketConfig.class)
                .with(TlsBucketConfig::protocols, Set.<String> of())
                .with(TlsBucketConfig::alpn, false)
                .with(TlsBucketConfig::pqcEnforcementPolicy, PqcEnforcementPolicy.STRICT)
                .build();
        return new VertxCertificateHolder(null, "test", config, new KeyStoreAndKeyCertOptions(store, OPTIONS), null);
    }

    private static KeyStore store(KeyPair leafKeys, Certificate... chain) throws Exception {
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, null);
        store.setKeyEntry("alias", leafKeys.getPrivate(), "password".toCharArray(), chain);
        return store;
    }

    private static KeyPair rsa() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static X509Certificate certificate(String subject, String issuer, KeyPair subjectKeys, PrivateKey signer)
            throws Exception {
        Date now = new Date();
        var builder = new JcaX509v3CertificateBuilder(new X500Name(issuer), BigInteger.valueOf(System.nanoTime()), now,
                new Date(now.getTime() + 86_400_000L), new X500Name(subject), subjectKeys.getPublic());
        return new JcaX509CertificateConverter()
                .getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(signer)));
    }
}
