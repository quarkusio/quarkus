package io.quarkus.test.mtls;

import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static org.bouncycastle.asn1.x509.Extension.authorityKeyIdentifier;
import static org.bouncycastle.asn1.x509.Extension.basicConstraints;
import static org.bouncycastle.asn1.x509.Extension.extendedKeyUsage;
import static org.bouncycastle.asn1.x509.Extension.keyUsage;
import static org.bouncycastle.asn1.x509.Extension.subjectAlternativeName;
import static org.bouncycastle.asn1.x509.Extension.subjectKeyIdentifier;
import static org.bouncycastle.asn1.x509.KeyPurposeId.id_kp_clientAuth;
import static org.bouncycastle.asn1.x509.KeyPurposeId.id_kp_serverAuth;
import static org.bouncycastle.asn1.x509.KeyUsage.cRLSign;
import static org.bouncycastle.asn1.x509.KeyUsage.digitalSignature;
import static org.bouncycastle.asn1.x509.KeyUsage.keyAgreement;
import static org.bouncycastle.asn1.x509.KeyUsage.keyCertSign;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.BigIntegers;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.mtls.MutualTLSConfig;
import io.quarkus.mtls.MutualTLSProvider;

@ApplicationScoped
@Named("test-mtls-provider")
public class TestMutualTLSProvider implements MutualTLSProvider {

    private static final int SERIAL_NUMBER_BITS = 20 * 8;
    private static final String SIG_ALG = "SHA256withECDSA";
    private static final SecureRandom random;
    private static final KeyPairGenerator keyPairGenerator;
    private static final CertificateFactory certFactory;
    private static final JcaX509ExtensionUtils extUtils;
    private static final Logger logger = Logger.getLogger(TestMutualTLSProvider.class);

    static {
        try {
            random = SecureRandom.getInstanceStrong();
            keyPairGenerator = KeyPairGenerator.getInstance("EC");
            keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
            certFactory = CertificateFactory.getInstance("X509");
            extUtils = new JcaX509ExtensionUtils();
        } catch (Exception e) {
            // Shouldn't happen
            throw new RuntimeException(e);
        }
    }

    private X509Certificate rootCA;
    private KeyPair intKeys;
    private X509Certificate intCA;
    private final boolean logCerts;

    private final ConcurrentHashMap<String, MutualTLSConfig> configs = new ConcurrentHashMap<>();

    @Inject
    public TestMutualTLSProvider(
            @ConfigProperty(name = "test-mtls-provider.print-certs", defaultValue = "false") boolean logCerts) {
        this.logCerts = logCerts;
        try {
            generateCertificateAuthorities();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MutualTLSConfig getConfig(String mutualTLSProviderName) {
        return configs.computeIfAbsent(mutualTLSProviderName, key -> {
            KeyPair idKeys;
            X509Certificate idCert;
            try {
                idKeys = keyPairGenerator.generateKeyPair();
                idCert = generateIdentityCertificate(new X500Name("CN=Provider " + mutualTLSProviderName),
                        idKeys.getPublic(),
                        intCA,
                        intKeys.getPrivate(),
                        new String[] { "localhost" },
                        Duration.ofSeconds(30));
                logCert("ID CERT: " + mutualTLSProviderName, idCert);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return new MutualTLSConfig(
                    asList(idCert, intCA, rootCA),
                    idKeys.getPrivate(),
                    asList(intCA, rootCA),
                    rootCA.getNotAfter().toInstant());
        });
    }

    private void generateCertificateAuthorities() throws Exception {
        KeyPair rootKeys = keyPairGenerator.generateKeyPair();
        rootCA = generateSelfSignedRootCA(new X500Name("CN=Test Root"), rootKeys, Duration.ofHours(12));
        logCert("ROOT CA", rootCA);

        intKeys = keyPairGenerator.generateKeyPair();
        intCA = generateIntermediateCA(new X500Name("CN=Test Intermediate"),
                intKeys.getPublic(),
                rootCA,
                rootKeys.getPrivate(),
                Duration.ofHours(11));
        logCert("INTERMEDIATE CA", intCA);
    }

    private void logCert(String name, X509Certificate cert) throws Exception {
        if (!logCerts) {
            return;
        }
        Base64.Encoder encoder = Base64.getMimeEncoder(64, "\n".getBytes());
        String pem = "-----BEGIN CERTIFICATE-----" + "\n" +
                encoder.encodeToString(cert.getEncoded()) + "\n" +
                "-----END CERTIFICATE-----";
        logger.info(name + "\n" + pem);
    }

    public static X509Certificate generateSelfSignedRootCA(X500Name subjectName, KeyPair subjectKeyPair, Duration ttl)
            throws Exception {
        var builder = start(subjectName, subjectKeyPair.getPublic(), subjectName, ttl, true, keyCertSign | cRLSign);
        return finish(builder, subjectKeyPair.getPrivate());
    }

    public static X509Certificate generateIntermediateCA(X500Name subjectName, PublicKey subjectKey,
            X509Certificate issuer, PrivateKey issuerKey, Duration ttl)
            throws Exception {
        var issuerName = X500Name.getInstance(issuer.getSubjectX500Principal().getEncoded());
        var authorityKeyId = extUtils.createAuthorityKeyIdentifier(issuer.getPublicKey());
        var builder = start(subjectName, subjectKey, issuerName, ttl, true, keyCertSign | cRLSign)
                .addExtension(authorityKeyIdentifier, false, authorityKeyId);
        return finish(builder, issuerKey);
    }

    public static X509Certificate generateIdentityCertificate(X500Name subjectName, PublicKey subjectKey,
            X509Certificate issuer, PrivateKey issuerKey,
            String[] sans,
            Duration ttl) throws Exception {
        var issuerName = X500Name.getInstance(issuer.getSubjectX500Principal().getEncoded());
        var authorityKeyId = extUtils.createAuthorityKeyIdentifier(issuer.getPublicKey());
        var extendedKeyUsageValue = new ExtendedKeyUsage(new KeyPurposeId[] { id_kp_serverAuth, id_kp_clientAuth });
        var subjAltNames = stream(sans).map(san -> new GeneralName(GeneralName.dNSName, san)).toArray(GeneralName[]::new);
        var builder = start(subjectName, subjectKey, issuerName, ttl, false, digitalSignature | keyAgreement)
                .addExtension(authorityKeyIdentifier, false, authorityKeyId)
                .addExtension(extendedKeyUsage, false, extendedKeyUsageValue)
                .addExtension(subjectAlternativeName, false, new GeneralNames(subjAltNames));
        return finish(builder, issuerKey);
    }

    private static X509v3CertificateBuilder start(X500Name subjectName, PublicKey subjectKey, X500Name issuerName,
            Duration ttl, boolean isCA, int keyUsageValue) throws Exception {
        var serial = generateSerialNumber();
        var notBefore = new Date(OffsetDateTime.now(UTC).toInstant().toEpochMilli());
        var notAfter = new Date(OffsetDateTime.now(UTC).plus(ttl).toInstant().toEpochMilli());
        var subjectKeyId = extUtils.createSubjectKeyIdentifier(subjectKey);
        return new JcaX509v3CertificateBuilder(issuerName, serial, notBefore, notAfter, subjectName, subjectKey)
                .addExtension(subjectKeyIdentifier, false, subjectKeyId)
                .addExtension(basicConstraints, true, new BasicConstraints(isCA))
                .addExtension(keyUsage, true, new KeyUsage(keyUsageValue));
    }

    private static X509Certificate finish(X509v3CertificateBuilder builder, PrivateKey key) throws Exception {
        var signer = new JcaContentSignerBuilder(SIG_ALG).build(key);
        var holder = builder.build(signer);
        var cert = certFactory.generateCertificate(new ByteArrayInputStream(holder.getEncoded()));
        return (X509Certificate) cert;
    }

    private static BigInteger generateSerialNumber() {
        return BigIntegers.createRandomBigInteger(SERIAL_NUMBER_BITS, random);
    }
}
