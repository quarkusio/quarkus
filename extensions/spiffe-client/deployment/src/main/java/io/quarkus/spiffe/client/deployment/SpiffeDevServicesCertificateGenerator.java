package io.quarkus.spiffe.client.deployment;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates X.509 certificates, CRLs, and key pairs for the SPIFFE dev service without external dependencies.
 * Certificates are built by constructing ASN.1 DER bytes directly and signing them with standard JCA,
 * avoiding the need for BouncyCastle or internal JDK APIs. Validity dates are set at generation time
 * to produce short-lived certificates matching real SPIRE behavior.
 */
final class SpiffeDevServicesCertificateGenerator {

    private static final long CA_DURATION_SECONDS = 86400;
    private static final long LEAF_DURATION_SECONDS = 3600;
    private static final DateTimeFormatter UTC_FMT = DateTimeFormatter.ofPattern("yyMMddHHmmss'Z'")
            .withZone(ZoneOffset.UTC);

    // pre-computed ASN.1 OID byte values (verified against base-128 encoding)
    private static final byte[] OID_COMMON_NAME = { 0x55, 0x04, 0x03 }; // 2.5.4.3
    private static final byte[] OID_BASIC_CONSTRAINTS = { 0x55, 0x1D, 0x13 }; // 2.5.29.19
    private static final byte[] OID_KEY_USAGE = { 0x55, 0x1D, 0x0F }; // 2.5.29.15
    private static final byte[] OID_EKU = { 0x55, 0x1D, 0x25 }; // 2.5.29.37
    private static final byte[] OID_SAN = { 0x55, 0x1D, 0x11 }; // 2.5.29.17
    private static final byte[] OID_SERVER_AUTH = { 0x2B, 0x06, 0x01, 0x05, 0x05, 0x07, 0x03, 0x01 }; // 1.3.6.1.5.5.7.3.1
    private static final byte[] OID_CLIENT_AUTH = { 0x2B, 0x06, 0x01, 0x05, 0x05, 0x07, 0x03, 0x02 }; // 1.3.6.1.5.5.7.3.2
    private static final byte[] OID_EC_SHA256 = { 0x2A, (byte) 0x86, 0x48, (byte) 0xCE, 0x3D, 0x04, 0x03, 0x02 }; // 1.2.840.10045.4.3.2
    private static final byte[] OID_RSA_SHA256 = { 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x0B }; // 1.2.840.113549.1.1.11

    private static final byte KEY_USAGE_DIGITAL_SIGNATURE = (byte) 0x80;
    private static final byte KEY_USAGE_CA = (byte) (0x80 | 0x04 | 0x02);

    private static final String CA_CN = "Quarkus SPIFFE Dev Services CA";
    private static final String WORKLOAD_CN = "Quarkus SPIFFE Dev Services Workload";
    private static final String INTERMEDIATE_CN_PREFIX = "Quarkus SPIFFE Dev Services Intermediate ";

    private final AtomicLong serialCounter = new AtomicLong(System.currentTimeMillis());

    SpiffeDevServicesCertificateGenerator() {
    }

    record CertAuthority(X509Certificate certificate, KeyPair keyPair) {
    }

    record WorkloadSvid(String spiffeId, List<X509Certificate> chain, PrivateKey privateKey,
            List<X509Certificate> trustBundle) {
    }

    CertAuthority createCertAuthority(String trustDomainSpiffeId, String algorithm) {
        KeyPair keyPair = generateKeyPair(algorithm);
        byte[] tbs = buildCaTbs(trustDomainSpiffeId, keyPair, algorithm, CA_DURATION_SECONDS);
        X509Certificate cert = signAndParse(tbs, keyPair.getPrivate(), algorithm);
        return new CertAuthority(cert, keyPair);
    }

    WorkloadSvid createWorkloadSvid(String spiffeId, CertAuthority ca, String algorithm) {
        KeyPair keyPair = generateKeyPair(algorithm);
        byte[] tbs = buildLeafTbs(spiffeId, keyPair, ca, algorithm, LEAF_DURATION_SECONDS);
        X509Certificate cert = signAndParse(tbs, ca.keyPair.getPrivate(), algorithm);
        return new WorkloadSvid(spiffeId, List.of(cert), keyPair.getPrivate(), List.of(ca.certificate));
    }

    WorkloadSvid createWorkloadSvidWithChain(String spiffeId, CertAuthority rootCa, String algorithm,
            int intermediateCount) {
        List<CertAuthority> intermediates = new ArrayList<>();
        CertAuthority parent = rootCa;
        for (int i = 0; i < intermediateCount; i++) {
            KeyPair interKeyPair = generateKeyPair(algorithm);
            byte[] interTbs = buildIntermediateCaTbs(INTERMEDIATE_CN_PREFIX + (i + 1), interKeyPair, parent, algorithm,
                    CA_DURATION_SECONDS);
            X509Certificate interCert = signAndParse(interTbs, parent.keyPair.getPrivate(), algorithm);
            CertAuthority intermediate = new CertAuthority(interCert, interKeyPair);
            intermediates.add(intermediate);
            parent = intermediate;
        }

        KeyPair leafKeyPair = generateKeyPair(algorithm);
        byte[] leafTbs = buildLeafTbs(spiffeId, leafKeyPair, parent, algorithm, LEAF_DURATION_SECONDS);
        X509Certificate leafCert = signAndParse(leafTbs, parent.keyPair.getPrivate(), algorithm);

        List<X509Certificate> chain = new ArrayList<>();
        chain.add(leafCert);
        for (int i = intermediates.size() - 1; i >= 0; i--) {
            chain.add(intermediates.get(i).certificate);
        }
        return new WorkloadSvid(spiffeId, List.copyOf(chain), leafKeyPair.getPrivate(), List.of(rootCa.certificate));
    }

    X509CRL createCrl(CertAuthority ca, String algorithm, BigInteger... revokedSerials) {
        byte[] tbsCrl = buildCrlTbs(ca, algorithm, CA_DURATION_SECONDS, revokedSerials);
        byte[] signature = sign(tbsCrl, ca.keyPair.getPrivate(), algorithm);
        byte[] crlDer = derSequence(tbsCrl, sigAlgIdentifier(algorithm), derBitString(signature));
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509CRL) cf.generateCRL(new ByteArrayInputStream(crlDer));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse generated CRL", e);
        }
    }

    private byte[] buildCaTbs(String spiffeId, KeyPair keyPair, String algorithm, long durationSeconds) {
        byte[] dn = buildDn(CA_CN);
        return derSequence(
                derExplicit(0, derInteger(2)),
                derInteger(nextSerial()),
                sigAlgIdentifier(algorithm),
                dn,
                buildValidity(durationSeconds),
                dn,
                keyPair.getPublic().getEncoded(),
                derExplicit(3, derSequence(
                        buildExtension(OID_BASIC_CONSTRAINTS, true, derSequence(derBoolean(true))),
                        buildExtension(OID_KEY_USAGE, true, derBitString(new byte[] { KEY_USAGE_CA })),
                        buildExtension(OID_SAN, true,
                                derSequence(derImplicit(6, spiffeId.getBytes(StandardCharsets.US_ASCII)))))));
    }

    private byte[] buildIntermediateCaTbs(String cn, KeyPair keyPair, CertAuthority issuer, String algorithm,
            long durationSeconds) {
        return derSequence(
                derExplicit(0, derInteger(2)),
                derInteger(nextSerial()),
                sigAlgIdentifier(algorithm),
                issuer.certificate.getSubjectX500Principal().getEncoded(),
                buildValidity(durationSeconds),
                buildDn(cn),
                keyPair.getPublic().getEncoded(),
                derExplicit(3, derSequence(
                        buildExtension(OID_BASIC_CONSTRAINTS, true, derSequence(derBoolean(true))),
                        buildExtension(OID_KEY_USAGE, true, derBitString(new byte[] { KEY_USAGE_CA })))));
    }

    private byte[] buildLeafTbs(String spiffeId, KeyPair keyPair, CertAuthority issuer, String algorithm,
            long durationSeconds) {
        return derSequence(
                derExplicit(0, derInteger(2)),
                derInteger(nextSerial()),
                sigAlgIdentifier(algorithm),
                issuer.certificate.getSubjectX500Principal().getEncoded(),
                buildValidity(durationSeconds),
                buildDn(WORKLOAD_CN),
                keyPair.getPublic().getEncoded(),
                derExplicit(3, derSequence(
                        buildExtension(OID_BASIC_CONSTRAINTS, true, derSequence(derBoolean(false))),
                        buildExtension(OID_KEY_USAGE, true,
                                derBitString(new byte[] { KEY_USAGE_DIGITAL_SIGNATURE })),
                        buildExtension(OID_EKU, false,
                                derSequence(derOid(OID_SERVER_AUTH), derOid(OID_CLIENT_AUTH))),
                        buildExtension(OID_SAN, false,
                                derSequence(derImplicit(6,
                                        spiffeId.getBytes(StandardCharsets.US_ASCII)))))));
    }

    private static byte[] buildCrlTbs(CertAuthority ca, String algorithm, long durationSeconds,
            BigInteger... revokedSerials) {
        byte[] issuerDn = ca.certificate.getSubjectX500Principal().getEncoded();
        Instant now = Instant.now();
        byte[] thisUpdate = UTC_FMT.format(now.minusSeconds(60)).getBytes(StandardCharsets.US_ASCII);
        byte[] nextUpdate = UTC_FMT.format(now.plusSeconds(durationSeconds)).getBytes(StandardCharsets.US_ASCII);

        List<byte[]> entries = new ArrayList<>();
        for (BigInteger serial : revokedSerials) {
            entries.add(derSequence(derInteger(serial), derUtcTime(thisUpdate)));
        }

        if (entries.isEmpty()) {
            return derSequence(
                    derInteger(1),
                    sigAlgIdentifier(algorithm),
                    issuerDn,
                    derUtcTime(thisUpdate),
                    derUtcTime(nextUpdate));
        }
        return derSequence(
                derInteger(1),
                sigAlgIdentifier(algorithm),
                issuerDn,
                derUtcTime(thisUpdate),
                derUtcTime(nextUpdate),
                derSequence(entries.toArray(new byte[0][])));
    }

    private static X509Certificate signAndParse(byte[] tbs, PrivateKey signingKey, String algorithm) {
        byte[] signature = sign(tbs, signingKey, algorithm);
        byte[] certDer = derSequence(tbs, sigAlgIdentifier(algorithm), derBitString(signature));
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certDer));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse generated certificate", e);
        }
    }

    private static byte[] sign(byte[] data, PrivateKey key, String algorithm) {
        try {
            Signature sig = Signature.getInstance(sigAlgorithmName(algorithm));
            sig.initSign(key);
            sig.update(data);
            return sig.sign();
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign", e);
        }
    }

    private static byte[] buildDn(String cn) {
        return derSequence(derSet(derSequence(derOid(OID_COMMON_NAME), derUtf8String(cn))));
    }

    private static byte[] buildValidity(long durationSeconds) {
        Instant now = Instant.now();
        byte[] notBefore = UTC_FMT.format(now.minusSeconds(60)).getBytes(StandardCharsets.US_ASCII);
        byte[] notAfter = UTC_FMT.format(now.plusSeconds(durationSeconds)).getBytes(StandardCharsets.US_ASCII);
        return derSequence(derUtcTime(notBefore), derUtcTime(notAfter));
    }

    private static byte[] buildExtension(byte[] oid, boolean critical, byte[] value) {
        if (critical) {
            return derSequence(derOid(oid), derBoolean(true), derOctetString(value));
        }
        return derSequence(derOid(oid), derOctetString(value));
    }

    private static byte[] sigAlgIdentifier(String algorithm) {
        if (algorithm.startsWith("ec-")) {
            return derSequence(derOid(OID_EC_SHA256));
        }
        return derSequence(derOid(OID_RSA_SHA256), new byte[] { 0x05, 0x00 });
    }

    private static String sigAlgorithmName(String algorithm) {
        return algorithm.startsWith("ec-") ? "SHA256withECDSA" : "SHA256WithRSA";
    }

    private static KeyPair generateKeyPair(String algorithm) {
        try {
            KeyPairGenerator kpg;
            switch (algorithm) {
                case "ec-p256" -> {
                    kpg = KeyPairGenerator.getInstance("EC");
                    kpg.initialize(new ECGenParameterSpec("secp256r1"));
                }
                case "ec-p384" -> {
                    kpg = KeyPairGenerator.getInstance("EC");
                    kpg.initialize(new ECGenParameterSpec("secp384r1"));
                }
                case "rsa-2048" -> {
                    kpg = KeyPairGenerator.getInstance("RSA");
                    kpg.initialize(2048);
                }
                default -> throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
            }
            return kpg.generateKeyPair();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate key pair", e);
        }
    }

    private BigInteger nextSerial() {
        return BigInteger.valueOf(serialCounter.getAndIncrement());
    }

    private static byte[] derSequence(byte[]... contents) {
        return derTagged(0x30, concat(contents));
    }

    private static byte[] derSet(byte[]... contents) {
        return derTagged(0x31, concat(contents));
    }

    private static byte[] derTagged(int tag, byte[] content) {
        byte[] len = derLength(content.length);
        byte[] result = new byte[1 + len.length + content.length];
        result[0] = (byte) tag;
        System.arraycopy(len, 0, result, 1, len.length);
        System.arraycopy(content, 0, result, 1 + len.length, content.length);
        return result;
    }

    private static byte[] derExplicit(int tagNum, byte[] content) {
        return derTagged(0xA0 | tagNum, content);
    }

    private static byte[] derImplicit(int tagNum, byte[] content) {
        return derTagged(0x80 | tagNum, content);
    }

    private static byte[] derInteger(BigInteger val) {
        return derTagged(0x02, val.toByteArray());
    }

    private static byte[] derInteger(int val) {
        return derInteger(BigInteger.valueOf(val));
    }

    private static byte[] derOid(byte[] encodedOid) {
        return derTagged(0x06, encodedOid);
    }

    private static byte[] derUtcTime(byte[] asciiTime) {
        return derTagged(0x17, asciiTime);
    }

    private static byte[] derUtf8String(String s) {
        return derTagged(0x0C, s.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] derBitString(byte[] data) {
        byte[] content = new byte[1 + data.length];
        System.arraycopy(data, 0, content, 1, data.length);
        return derTagged(0x03, content);
    }

    private static byte[] derOctetString(byte[] data) {
        return derTagged(0x04, data);
    }

    private static byte[] derBoolean(boolean val) {
        return derTagged(0x01, new byte[] { val ? (byte) 0xFF : 0x00 });
    }

    private static byte[] derLength(int length) {
        if (length < 128) {
            return new byte[] { (byte) length };
        } else if (length < 256) {
            return new byte[] { (byte) 0x81, (byte) length };
        } else {
            return new byte[] { (byte) 0x82, (byte) (length >> 8), (byte) (length & 0xFF) };
        }
    }

    private static byte[] concat(byte[]... arrays) {
        int total = 0;
        for (byte[] a : arrays) {
            total += a.length;
        }
        byte[] result = new byte[total];
        int offset = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, result, offset, a.length);
            offset += a.length;
        }
        return result;
    }
}
