package io.quarkus.vault;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.shaded.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.pkcs_9_at_extensionRequest;
import static org.testcontainers.shaded.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.rsaEncryption;
import static org.testcontainers.shaded.org.bouncycastle.asn1.x509.Extension.nameConstraints;
import static org.testcontainers.shaded.org.bouncycastle.asn1.x509.Extension.subjectAlternativeName;

import java.io.StringReader;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.shaded.org.bouncycastle.asn1.ASN1Encodable;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.BasicConstraints;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.Extensions;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.GeneralName;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.GeneralNames;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.GeneralSubtree;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.NameConstraints;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.testcontainers.shaded.org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.testcontainers.shaded.org.bouncycastle.cert.X509CertificateHolder;
import org.testcontainers.shaded.org.bouncycastle.openssl.PEMParser;
import org.testcontainers.shaded.org.bouncycastle.pkcs.PKCS10CertificationRequest;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vault.pki.CAChainData;
import io.quarkus.vault.pki.CRLData;
import io.quarkus.vault.pki.CertificateData;
import io.quarkus.vault.pki.CertificateExtendedKeyUsage;
import io.quarkus.vault.pki.CertificateKeyType;
import io.quarkus.vault.pki.CertificateKeyUsage;
import io.quarkus.vault.pki.ConfigCRLOptions;
import io.quarkus.vault.pki.ConfigURLsOptions;
import io.quarkus.vault.pki.DataFormat;
import io.quarkus.vault.pki.GenerateCertificateOptions;
import io.quarkus.vault.pki.GenerateIntermediateCSROptions;
import io.quarkus.vault.pki.GenerateRootOptions;
import io.quarkus.vault.pki.GeneratedCertificate;
import io.quarkus.vault.pki.GeneratedIntermediateCSRResult;
import io.quarkus.vault.pki.GeneratedRootCertificate;
import io.quarkus.vault.pki.RoleOptions;
import io.quarkus.vault.pki.SignIntermediateCAOptions;
import io.quarkus.vault.pki.SignedCertificate;
import io.quarkus.vault.pki.TidyOptions;

public class VaultPKIITCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-vault-pki.properties", "application.properties"));

    @Inject
    VaultPKISecretEngine pkiSecretEngine;

    @Inject
    VaultPKISecretEngineFactory pkiSecretEngineFactory;

    @AfterEach
    public void cleanup() {
        try {
            pkiSecretEngine.deleteRoot();
        } catch (Throwable x) {
            // ignore
        }

        try {
            pkiSecretEngineFactory.engine("pki2").deleteRoot();
        } catch (Throwable x) {
            // ignore
        }

        try {
            for (String role : pkiSecretEngine.getRoles()) {
                try {
                    pkiSecretEngine.deleteRole(role);
                } catch (Throwable x) {
                    // ignore
                }
            }
        } catch (Throwable x) {
            // ignore
        }
    }

    @Test
    public void testGenerateRootOptions() throws Exception {
        GenerateRootOptions options = new GenerateRootOptions();
        options.subjectCommonName = "test.example.com";
        options.subjectOrganization = "Test Org";
        options.subjectOrganizationalUnit = "Test Unit";
        options.subjectStreetAddress = "123 Main Street";
        options.subjectLocality = "New York";
        options.subjectProvince = "NY";
        options.subjectCountry = "USA";
        options.subjectPostalCode = "10030";
        options.subjectSerialNumber = "9876543210";
        options.subjectAlternativeNames = singletonList("alt.example.com");
        options.ipSubjectAlternativeNames = singletonList("1.2.3.4");
        options.uriSubjectAlternativeNames = singletonList("ex:12345");
        options.otherSubjectAlternativeNames = singletonList("1.3.6.1.4.1.311.20.2.3;UTF8:test");
        options.excludeCommonNameFromSubjectAlternativeNames = true;
        options.timeToLive = "8760h";
        options.keyType = CertificateKeyType.EC;
        options.keyBits = 256;
        options.exportPrivateKey = true;
        options.maxPathLength = 3;
        options.permittedDnsDomains = asList("subs1.example.com", "subs2.example.com");

        GeneratedRootCertificate result = pkiSecretEngine.generateRoot(options);

        assertEquals(DataFormat.PEM, result.certificate.getFormat());
        assertNotNull(result.certificate.getData());
        assertFalse(result.certificate.getData().toString().isEmpty());
        assertDoesNotThrow(() -> result.certificate.getCertificate());

        X509CertificateHolder certificate = (X509CertificateHolder) new PEMParser(
                new StringReader((String) result.certificate.getData()))
                        .readObject();

        assertEquals(DataFormat.PEM, result.issuingCA.getFormat());
        assertNotNull(result.issuingCA.getData());
        assertFalse(result.issuingCA.getData().toString().isEmpty());
        assertDoesNotThrow(() -> result.issuingCA.getCertificate());

        X509CertificateHolder issuingCA = (X509CertificateHolder) new PEMParser(
                new StringReader((String) result.issuingCA.getData()))
                        .readObject();

        // Check all subject name component options
        assertEquals(
                "C=USA,ST=NY,L=New York,STREET=123 Main Street,PostalCode=10030," +
                        "O=Test Org,OU=Test Unit,CN=test.example.com,SERIALNUMBER=9876543210",
                certificate.getSubject().toString());

        // Check subjectAlternativeNames, ipSubjectAlternativeNames, uriSubjectAlternativeNames,
        // otherSubjectAlternativeNames & excludeCommonNameFromSubjectAlternativeNames options
        assertNotNull(certificate.getExtension(subjectAlternativeName));
        GeneralNames generalNames = GeneralNames.getInstance(certificate.getExtension(subjectAlternativeName).getParsedValue());
        List<String> subjectAlternativeNames = Arrays.stream(generalNames.getNames())
                .map(GeneralName::getName)
                .map(ASN1Encodable::toString)
                .collect(toList());
        assertEquals(asList("[1.3.6.1.4.1.311.20.2.3, [0]test]", "alt.example.com", "#01020304", "ex:12345"),
                subjectAlternativeNames);

        // Check timeToLive option
        assertEquals(8759, Duration.between(Instant.now().plusSeconds(30), certificate.getNotAfter().toInstant()).toHours());

        // Check keyType option
        SubjectPublicKeyInfo subPKI = certificate.getSubjectPublicKeyInfo();
        assertEquals(X9ObjectIdentifiers.id_ecPublicKey, subPKI.getAlgorithm().getAlgorithm());

        // Check keyBits option
        assertEquals(65, subPKI.getPublicKeyData().getOctets().length);

        // Check maxPathLength option
        BasicConstraints basicCons = BasicConstraints.fromExtensions(certificate.getExtensions());
        assertEquals(BigInteger.valueOf(3), basicCons.getPathLenConstraint());

        // Check permittedDnsDomains option
        assertNotNull(certificate.getExtension(nameConstraints));
        NameConstraints nameCons = NameConstraints.getInstance(certificate.getExtension(nameConstraints).getParsedValue());
        List<String> permittedDnsDomains = Arrays.stream(nameCons.getPermittedSubtrees())
                .map(GeneralSubtree::getBase)
                .map(GeneralName::getName)
                .map(ASN1Encodable::toString)
                .collect(toList());
        assertEquals(asList("subs1.example.com", "subs2.example.com"), permittedDnsDomains);

        // Check returned cert is self-signed
        assertEquals(certificate.getSubject(), issuingCA.getSubject());

        // Check returned a serial number
        assertNotNull(result.serialNumber);
        assertFalse(result.serialNumber.isEmpty());

        // Check private key
        assertNotNull(result.privateKey);
        assertEquals(DataFormat.PEM, result.privateKey.getFormat());
        assertTrue(result.privateKey.isPKCS8());
        assertNotNull(result.privateKey.getData());
        assertFalse(result.privateKey.getData().toString().isEmpty());
        assertDoesNotThrow(() -> result.privateKey.getKeySpec());
    }

    @Test
    public void deleteRoot() {
        GenerateRootOptions options = new GenerateRootOptions();
        options.subjectCommonName = "test.example.com";

        GeneratedRootCertificate result = pkiSecretEngine.generateRoot(options);
        assertNotNull(result.certificate);

        assertDoesNotThrow(() -> pkiSecretEngine.deleteRoot());
    }

    @Test
    public void testGenerateIntermediateCSROptions() throws Exception {
        GenerateIntermediateCSROptions options = new GenerateIntermediateCSROptions();
        options.subjectCommonName = "test.example.com";
        options.subjectOrganization = "Test Org";
        options.subjectOrganizationalUnit = "Test Unit";
        options.subjectStreetAddress = "123 Main Street";
        options.subjectLocality = "New York";
        options.subjectProvince = "NY";
        options.subjectCountry = "USA";
        options.subjectPostalCode = "10030";
        options.subjectSerialNumber = "9876543210";
        options.subjectAlternativeNames = singletonList("alt.example.com");
        options.ipSubjectAlternativeNames = singletonList("1.2.3.4");
        options.uriSubjectAlternativeNames = singletonList("ex:12345");
        options.otherSubjectAlternativeNames = singletonList("1.3.6.1.4.1.311.20.2.3;UTF8:test");
        options.excludeCommonNameFromSubjectAlternativeNames = true;
        options.keyType = CertificateKeyType.EC;
        options.keyBits = 256;
        options.exportPrivateKey = true;

        GeneratedIntermediateCSRResult result = pkiSecretEngine.generateIntermediateCSR(options);

        assertEquals(DataFormat.PEM, result.csr.getFormat());
        assertNotNull(result.csr.getData());
        assertFalse(result.csr.getData().toString().isEmpty());

        PKCS10CertificationRequest csr = (PKCS10CertificationRequest) new PEMParser(
                new StringReader((String) result.csr.getData())).readObject();
        // Check all subject name component options
        assertEquals(
                "C=USA,ST=NY,L=New York,STREET=123 Main Street,PostalCode=10030," +
                        "O=Test Org,OU=Test Unit,CN=test.example.com,SERIALNUMBER=9876543210",
                csr.getSubject().toString());

        // Check subjectAlternativeNames, ipSubjectAlternativeNames, uriSubjectAlternativeNames,
        // otherSubjectAlternativeNames & excludeCommonNameFromSubjectAlternativeNames options
        assertEquals(1, csr.getAttributes(pkcs_9_at_extensionRequest).length);
        Extensions extReq = Extensions.getInstance(csr.getAttributes(pkcs_9_at_extensionRequest)[0].getAttributeValues()[0]);

        assertNotNull(extReq.getExtension(subjectAlternativeName));
        GeneralNames generalNames = GeneralNames.getInstance(extReq.getExtension(subjectAlternativeName).getParsedValue());
        List<String> subjectAlternativeNames = Arrays.stream(generalNames.getNames())
                .map(GeneralName::getName)
                .map(ASN1Encodable::toString)
                .collect(toList());
        assertEquals(asList("[1.3.6.1.4.1.311.20.2.3, [0]test]", "alt.example.com", "#01020304", "ex:12345"),
                subjectAlternativeNames);

        // Check keyType option
        SubjectPublicKeyInfo subPKI = csr.getSubjectPublicKeyInfo();
        assertEquals(X9ObjectIdentifiers.id_ecPublicKey, subPKI.getAlgorithm().getAlgorithm());

        // Check keyBits option
        assertEquals(65, subPKI.getPublicKeyData().getOctets().length);

        // Check private key
        assertNotNull(result.privateKey);
        assertEquals(DataFormat.PEM, result.privateKey.getFormat());
        assertTrue(result.privateKey.isPKCS8());
        assertNotNull(result.privateKey.getData());
        assertFalse(result.privateKey.getData().toString().isEmpty());
        assertDoesNotThrow(() -> result.privateKey.getKeySpec());
    }

    @Test
    public void testSignIntermediateCAOptions() throws Exception {
        // Generate root CA in "pki"
        GenerateRootOptions genRootOptions = new GenerateRootOptions();
        genRootOptions.subjectCommonName = "root.example.com";

        GeneratedRootCertificate generatedRoot = pkiSecretEngine.generateRoot(genRootOptions);
        assertNotNull(generatedRoot.certificate);

        // Generate intermediate CA CSR in "pki2"
        VaultPKISecretEngine pkiSecretEngine2 = pkiSecretEngineFactory.engine("pki2");

        GenerateIntermediateCSROptions genIntCSROptions = new GenerateIntermediateCSROptions();
        genIntCSROptions.subjectCommonName = "test1.example.com";

        GeneratedIntermediateCSRResult csrResult = pkiSecretEngine2.generateIntermediateCSR(genIntCSROptions);
        assertNotNull(csrResult.csr);

        // Sign the intermediate CA using "pki"
        SignIntermediateCAOptions options = new SignIntermediateCAOptions();
        options.subjectCommonName = "test.example.com";
        options.subjectOrganization = "Test Org";
        options.subjectOrganizationalUnit = "Test Unit";
        options.subjectStreetAddress = "123 Main Street";
        options.subjectLocality = "New York";
        options.subjectProvince = "NY";
        options.subjectCountry = "USA";
        options.subjectPostalCode = "10030";
        options.subjectSerialNumber = "9876543210";
        options.subjectAlternativeNames = singletonList("alt.example.com");
        options.ipSubjectAlternativeNames = singletonList("1.2.3.4");
        options.uriSubjectAlternativeNames = singletonList("ex:12345");
        //options.otherSubjectAlternativeNames = singletonList("1.3.6.1.4.1.311.20.2.3;UTF8:test");
        options.excludeCommonNameFromSubjectAlternativeNames = true;
        options.timeToLive = "8760h";
        options.maxPathLength = 3;
        options.permittedDnsDomains = asList("subs1.example.com", "subs2.example.com");
        options.useCSRValues = false;

        SignedCertificate result = pkiSecretEngine.signIntermediateCA((String) csrResult.csr.getData(), options);

        assertEquals(DataFormat.PEM, result.certificate.getFormat());
        assertNotNull(result.certificate.getData());
        assertFalse(result.certificate.getData().toString().isEmpty());
        assertDoesNotThrow(() -> result.certificate.getCertificate());

        X509CertificateHolder certificate = (X509CertificateHolder) new PEMParser(
                new StringReader((String) result.certificate.getData()))
                        .readObject();
        X509CertificateHolder issuingCA = (X509CertificateHolder) new PEMParser(
                new StringReader((String) result.issuingCA.getData()))
                        .readObject();

        // Check all subject name component options
        assertEquals(
                "C=USA,ST=NY,L=New York,STREET=123 Main Street,PostalCode=10030," +
                        "O=Test Org,OU=Test Unit,CN=test.example.com,SERIALNUMBER=9876543210",
                certificate.getSubject().toString());

        // Check subjectAlternativeNames, ipSubjectAlternativeNames, uriSubjectAlternativeNames,
        // otherSubjectAlternativeNames & excludeCommonNameFromSubjectAlternativeNames options
        assertNotNull(certificate.getExtension(subjectAlternativeName));
        GeneralNames generalNames = GeneralNames.getInstance(certificate.getExtension(subjectAlternativeName).getParsedValue());
        List<String> subjectAlternativeNames = Arrays.stream(generalNames.getNames())
                .map(GeneralName::getName)
                .map(ASN1Encodable::toString)
                .collect(toList());
        assertEquals(asList("alt.example.com", "#01020304", "ex:12345"),
                subjectAlternativeNames);

        // Check timeToLive option
        assertEquals(8759, Duration.between(Instant.now().plusSeconds(30), certificate.getNotAfter().toInstant()).toHours());

        // Check keyType option
        SubjectPublicKeyInfo subPKI = certificate.getSubjectPublicKeyInfo();
        assertEquals(rsaEncryption, subPKI.getAlgorithm().getAlgorithm());

        // Check keyBits option
        assertEquals(270, subPKI.getPublicKeyData().getOctets().length);

        // Check maxPathLength option
        BasicConstraints basicCons = BasicConstraints.fromExtensions(certificate.getExtensions());
        assertEquals(BigInteger.valueOf(3), basicCons.getPathLenConstraint());

        // Check permittedDnsDomains option
        assertNotNull(certificate.getExtension(nameConstraints));
        NameConstraints nameCons = NameConstraints.getInstance(certificate.getExtension(nameConstraints).getParsedValue());
        List<String> permittedDnsDomains = Arrays.stream(nameCons.getPermittedSubtrees())
                .map(GeneralSubtree::getBase)
                .map(GeneralName::getName)
                .map(ASN1Encodable::toString)
                .collect(toList());
        assertEquals(asList("subs1.example.com", "subs2.example.com"), permittedDnsDomains);

        // Check returned cert is self-signed
        assertEquals("CN=root.example.com", issuingCA.getSubject().toString());

        // Check returned a serial number
        assertNotNull(result.serialNumber);
        assertFalse(result.serialNumber.isEmpty());
    }

    @Test
    public void testSetSignedIntermediaCA() throws Exception {
        // Generate root CA in "pki"
        GenerateRootOptions genRootOptions = new GenerateRootOptions();
        genRootOptions.subjectCommonName = "root.example.com";

        GeneratedRootCertificate generatedRoot = pkiSecretEngine.generateRoot(genRootOptions);
        assertNotNull(generatedRoot.certificate);

        // Generate intermediate CA CSR in "pki2"
        VaultPKISecretEngine pkiSecretEngine2 = pkiSecretEngineFactory.engine("pki2");

        GenerateIntermediateCSROptions genIntCSROptions = new GenerateIntermediateCSROptions();
        genIntCSROptions.subjectCommonName = "test1.example.com";

        GeneratedIntermediateCSRResult csrResult = pkiSecretEngine2.generateIntermediateCSR(genIntCSROptions);

        assertNotNull(csrResult.csr);
        assertEquals(DataFormat.PEM, csrResult.csr.getFormat());
        assertNotNull(csrResult.csr.getData());
        assertFalse(csrResult.csr.getData().toString().isEmpty());

        // Sign the intermediate CA using "pki"
        SignIntermediateCAOptions options = new SignIntermediateCAOptions();
        SignedCertificate result = pkiSecretEngine.signIntermediateCA((String) csrResult.csr.getData(), options);

        // Set signed intermediate CA into "pki2"
        pkiSecretEngine2.setSignedIntermediateCA((String) result.certificate.getData());

        // Get CA cert and check subject (PEM)
        X509CertificateHolder certificate = (X509CertificateHolder) new PEMParser(
                new StringReader(pkiSecretEngine2.getCertificateAuthority().getData())).readObject();

        assertEquals("CN=test1.example.com", certificate.getSubject().toString());
    }

    @Test
    public void testUpdateAndGetRole() {
        // Test all non-default values (except generateLease due to conflict with noStore)
        RoleOptions options = new RoleOptions();
        options.timeToLive = "150m";
        options.maxTimeToLive = "300m";
        options.allowLocalhost = false;
        options.allowedDomains = asList("a.example.com", "b.example.com");
        options.allowTemplatesInAllowedDomains = true;
        options.allowBareDomains = true;
        options.allowSubdomains = true;
        options.allowGlobsInAllowedDomains = true;
        options.allowAnyName = true;
        options.enforceHostnames = false;
        options.allowIpSubjectAlternativeNames = false;
        options.allowedUriSubjectAlternativeNames = asList("ex:54321", "ex:12345");
        options.allowedOtherSubjectAlternativeNames = singletonList("1.3.6.1.4.1.311.20.2.3;UTF8:test");
        options.serverFlag = false;
        options.clientFlag = false;
        options.codeSigningFlag = true;
        options.emailProtectionFlag = true;
        options.keyType = CertificateKeyType.EC;
        options.keyBits = 256;
        options.keyUsages = asList(CertificateKeyUsage.CertSign, CertificateKeyUsage.CRLSign);
        options.extendedKeyUsages = asList(CertificateExtendedKeyUsage.ClientAuth, CertificateExtendedKeyUsage.CodeSigning);
        options.extendedKeyUsageOIDs = emptyList();
        options.useCSRCommonName = false;
        options.useCSRSubjectAlternativeNames = false;
        options.subjectOrganization = "Test Org";
        options.subjectOrganizationalUnit = "Test Unit";
        options.subjectStreetAddress = "123 Main Street";
        options.subjectLocality = "New York";
        options.subjectProvince = "NY";
        options.subjectCountry = "USA";
        options.subjectPostalCode = "10030";
        options.allowedSubjectSerialNumbers = asList("*_9876543210", "9876543210_*");
        options.generateLease = false;
        options.noStore = true;
        options.requireCommonName = false;
        options.basicConstraintsValidForNonCA = true;
        options.notBeforeDuration = "90s";

        pkiSecretEngine.updateRole("test", options);

        RoleOptions result = pkiSecretEngine.getRole("test");
        assertEquals("9000", result.timeToLive);
        assertEquals("18000", result.maxTimeToLive);
        assertEquals(options.allowLocalhost, result.allowLocalhost);
        assertEquals(options.allowedDomains, result.allowedDomains);
        assertEquals(options.allowTemplatesInAllowedDomains, result.allowTemplatesInAllowedDomains);
        assertEquals(options.allowBareDomains, result.allowBareDomains);
        assertEquals(options.allowSubdomains, result.allowSubdomains);
        assertEquals(options.allowGlobsInAllowedDomains, result.allowGlobsInAllowedDomains);
        assertEquals(options.allowAnyName, result.allowAnyName);
        assertEquals(options.enforceHostnames, result.enforceHostnames);
        assertEquals(options.allowIpSubjectAlternativeNames, result.allowIpSubjectAlternativeNames);
        assertEquals(options.allowedUriSubjectAlternativeNames, result.allowedUriSubjectAlternativeNames);
        assertEquals(options.allowedOtherSubjectAlternativeNames, result.allowedOtherSubjectAlternativeNames);
        assertEquals(options.serverFlag, result.serverFlag);
        assertEquals(options.clientFlag, result.clientFlag);
        assertEquals(options.codeSigningFlag, result.codeSigningFlag);
        assertEquals(options.emailProtectionFlag, result.emailProtectionFlag);
        assertEquals(options.keyType, result.keyType);
        assertEquals(options.keyBits, result.keyBits);
        assertEquals(options.keyUsages, result.keyUsages);
        assertEquals(options.extendedKeyUsages, result.extendedKeyUsages);
        assertEquals(options.extendedKeyUsageOIDs, result.extendedKeyUsageOIDs);
        assertEquals(options.useCSRCommonName, result.useCSRCommonName);
        assertEquals(options.useCSRSubjectAlternativeNames, result.useCSRSubjectAlternativeNames);
        assertEquals(options.subjectOrganization, result.subjectOrganization);
        assertEquals(options.subjectOrganizationalUnit, result.subjectOrganizationalUnit);
        assertEquals(options.subjectStreetAddress, result.subjectStreetAddress);
        assertEquals(options.subjectLocality, result.subjectLocality);
        assertEquals(options.subjectProvince, result.subjectProvince);
        assertEquals(options.subjectCountry, result.subjectCountry);
        assertEquals(options.subjectPostalCode, result.subjectPostalCode);
        assertEquals(options.allowedSubjectSerialNumbers, result.allowedSubjectSerialNumbers);
        assertEquals(options.generateLease, result.generateLease);
        assertEquals(options.noStore, result.noStore);
        assertEquals(options.requireCommonName, result.requireCommonName);
        assertEquals(options.basicConstraintsValidForNonCA, result.basicConstraintsValidForNonCA);
        assertEquals("90", result.notBeforeDuration);

        // Test non-default generateLease option
        RoleOptions options2 = new RoleOptions();
        options2.generateLease = true;
        options2.noStore = false;

        pkiSecretEngine.updateRole("test", options2);

        RoleOptions result2 = pkiSecretEngine.getRole("test");
        assertEquals(true, result2.generateLease);
    }

    @Test
    public void testListRoles() {
        RoleOptions options = new RoleOptions();
        pkiSecretEngine.updateRole("test1", options);
        pkiSecretEngine.updateRole("test2", options);

        assertEquals(asList("test1", "test2"), pkiSecretEngine.getRoles());
    }

    @Test
    public void testDeleteRole() {
        RoleOptions options = new RoleOptions();
        pkiSecretEngine.updateRole("test1", options);

        assertEquals(singletonList("test1"), pkiSecretEngine.getRoles());

        pkiSecretEngine.deleteRole("test1");

        assertEquals(emptyList(), pkiSecretEngine.getRoles());
    }

    @Test
    public void testGenerateCertificate() throws Exception {
        // Generate root
        GenerateRootOptions generateRootOptions = new GenerateRootOptions();
        generateRootOptions.subjectCommonName = "root.example.com";
        generateRootOptions.timeToLive = "24h";
        assertNotNull(pkiSecretEngine.generateRoot(generateRootOptions).certificate);

        // Generate role
        RoleOptions roleOptions = new RoleOptions();
        roleOptions.allowedDomains = singletonList("example.com");
        roleOptions.allowSubdomains = true;
        roleOptions.allowIpSubjectAlternativeNames = true;
        roleOptions.allowedUriSubjectAlternativeNames = singletonList("ex:*");
        roleOptions.allowedOtherSubjectAlternativeNames = singletonList("1.3.6.1.4.1.311.20.2.3;UTF8:*");
        pkiSecretEngine.updateRole("test", roleOptions);

        // Test cert generation
        GenerateCertificateOptions options = new GenerateCertificateOptions();
        options.subjectCommonName = "test.example.com";
        options.subjectAlternativeNames = singletonList("alt.example.com");
        options.ipSubjectAlternativeNames = singletonList("1.2.3.4");
        options.uriSubjectAlternativeNames = singletonList("ex:12345");
        options.otherSubjectAlternativeNames = singletonList("1.3.6.1.4.1.311.20.2.3;UTF8:test");
        options.excludeCommonNameFromSubjectAlternativeNames = true;
        options.timeToLive = "333m";

        GeneratedCertificate result = pkiSecretEngine.generateCertificate("test", options);

        assertEquals(DataFormat.PEM, result.certificate.getFormat());
        assertNotNull(result.certificate.getData());
        assertFalse(result.certificate.getData().toString().isEmpty());
        assertDoesNotThrow(() -> result.certificate.getCertificate());

        X509CertificateHolder certificate = (X509CertificateHolder) new PEMParser(
                new StringReader((String) result.certificate.getData()))
                        .readObject();

        assertEquals(DataFormat.PEM, result.issuingCA.getFormat());
        assertNotNull(result.issuingCA.getData());
        assertFalse(result.issuingCA.getData().toString().isEmpty());
        assertDoesNotThrow(() -> result.issuingCA.getCertificate());

        X509CertificateHolder issuingCA = (X509CertificateHolder) new PEMParser(
                new StringReader((String) result.issuingCA.getData()))
                        .readObject();

        // Check all subject name component options
        assertEquals("CN=test.example.com", certificate.getSubject().toString());

        // Check subjectAlternativeNames, ipSubjectAlternativeNames, uriSubjectAlternativeNames,
        // otherSubjectAlternativeNames & excludeCommonNameFromSubjectAlternativeNames options
        assertNotNull(certificate.getExtension(subjectAlternativeName));
        GeneralNames generalNames = GeneralNames.getInstance(certificate.getExtension(subjectAlternativeName).getParsedValue());
        List<String> subjectAlternativeNames = Arrays.stream(generalNames.getNames())
                .map(GeneralName::getName)
                .map(ASN1Encodable::toString)
                .collect(toList());
        assertEquals(asList("[1.3.6.1.4.1.311.20.2.3, [0]test]", "alt.example.com", "#01020304", "ex:12345"),
                subjectAlternativeNames);

        // Check timeToLive option
        assertEquals(332, Duration.between(Instant.now().plusSeconds(30), certificate.getNotAfter().toInstant()).toMinutes());

        // Check returned cert is not self-signed
        assertNotEquals(certificate.getSubject(), issuingCA.getSubject());

        // Check returned a serial number
        assertNotNull(result.serialNumber);
        assertFalse(result.serialNumber.isEmpty());

        // Check returned a private key type
        assertEquals(CertificateKeyType.RSA, result.privateKeyType);

        // Check returned private key
        assertNotNull(result.privateKey);
        assertEquals(DataFormat.PEM, result.privateKey.getFormat());
        assertTrue(result.privateKey.isPKCS8());
        assertNotNull(result.privateKey.getData());
        assertFalse(result.privateKey.getData().toString().isEmpty());
        assertDoesNotThrow(() -> result.privateKey.getKeySpec());
    }

    @Test
    public void testGenerateCertificateDer() throws Exception {
        // Generate root
        GenerateRootOptions generateRootOptions = new GenerateRootOptions();
        generateRootOptions.subjectCommonName = "root.example.com";
        generateRootOptions.timeToLive = "24h";
        assertNotNull(pkiSecretEngine.generateRoot(generateRootOptions).certificate);

        // Generate role
        RoleOptions roleOptions = new RoleOptions();
        roleOptions.allowedDomains = singletonList("example.com");
        roleOptions.allowSubdomains = true;
        roleOptions.allowIpSubjectAlternativeNames = true;
        roleOptions.allowedUriSubjectAlternativeNames = singletonList("ex:*");
        roleOptions.allowedOtherSubjectAlternativeNames = singletonList("1.3.6.1.4.1.311.20.2.3;UTF8:*");
        pkiSecretEngine.updateRole("test", roleOptions);

        // Test cert generation
        GenerateCertificateOptions options = new GenerateCertificateOptions();
        options.subjectCommonName = "test.example.com";
        options.subjectAlternativeNames = singletonList("alt.example.com");
        options.ipSubjectAlternativeNames = singletonList("1.2.3.4");
        options.uriSubjectAlternativeNames = singletonList("ex:12345");
        options.otherSubjectAlternativeNames = singletonList("1.3.6.1.4.1.311.20.2.3;UTF8:test");
        options.excludeCommonNameFromSubjectAlternativeNames = true;
        options.timeToLive = "333m";
        options.format = DataFormat.DER;

        GeneratedCertificate result = pkiSecretEngine.generateCertificate("test", options);

        assertEquals(DataFormat.DER, result.certificate.getFormat());
        assertNotNull(result.certificate.getData());
        assertFalse(result.certificate.getData().toString().isEmpty());
        assertDoesNotThrow(() -> result.certificate.getCertificate());

        X509CertificateHolder certificate = new X509CertificateHolder((byte[]) result.certificate.getData());

        assertEquals(DataFormat.DER, result.issuingCA.getFormat());
        assertNotNull(result.issuingCA.getData());
        assertFalse(result.issuingCA.getData().toString().isEmpty());
        assertDoesNotThrow(() -> result.issuingCA.getCertificate());

        X509CertificateHolder issuingCA = new X509CertificateHolder((byte[]) result.issuingCA.getData());

        // Check all subject name component options
        assertEquals("CN=test.example.com", certificate.getSubject().toString());

        // Check subjectAlternativeNames, ipSubjectAlternativeNames, uriSubjectAlternativeNames,
        // otherSubjectAlternativeNames & excludeCommonNameFromSubjectAlternativeNames options
        assertNotNull(certificate.getExtension(subjectAlternativeName));
        GeneralNames generalNames = GeneralNames.getInstance(certificate.getExtension(subjectAlternativeName).getParsedValue());
        List<String> subjectAlternativeNames = Arrays.stream(generalNames.getNames())
                .map(GeneralName::getName)
                .map(ASN1Encodable::toString)
                .collect(toList());
        assertEquals(asList("[1.3.6.1.4.1.311.20.2.3, [0]test]", "alt.example.com", "#01020304", "ex:12345"),
                subjectAlternativeNames);

        // Check timeToLive option
        assertEquals(332, Duration.between(Instant.now().plusSeconds(30), certificate.getNotAfter().toInstant()).toMinutes());

        // Check returned cert is not self-signed
        assertNotEquals(certificate.getSubject(), issuingCA.getSubject());

        // Check returned a serial number
        assertNotNull(result.serialNumber);
        assertFalse(result.serialNumber.isEmpty());

        // Check returned a private key type
        assertEquals(CertificateKeyType.RSA, result.privateKeyType);

        // Check returned private key
        assertNotNull(result.privateKey);
        assertEquals(DataFormat.DER, result.privateKey.getFormat());
        assertTrue(result.privateKey.isPKCS8());
        assertNotNull(result.privateKey.getData());
        assertFalse(result.privateKey.getData().toString().isEmpty());
        assertDoesNotThrow(() -> result.privateKey.getKeySpec());
    }

    @Test
    public void testSignCSR() throws Exception {
        // Generate root
        GenerateRootOptions generateRootOptions = new GenerateRootOptions();
        generateRootOptions.subjectCommonName = "root.example.com";
        generateRootOptions.timeToLive = "24h";
        assertNotNull(pkiSecretEngine.generateRoot(generateRootOptions).certificate);

        // Generate role
        RoleOptions roleOptions = new RoleOptions();
        roleOptions.allowedDomains = singletonList("example.com");
        roleOptions.allowSubdomains = true;
        roleOptions.allowIpSubjectAlternativeNames = true;
        roleOptions.allowedUriSubjectAlternativeNames = singletonList("ex:*");
        roleOptions.allowedOtherSubjectAlternativeNames = singletonList("1.3.6.1.4.1.311.20.2.3;UTF8:*");
        roleOptions.useCSRCommonName = false;
        roleOptions.useCSRSubjectAlternativeNames = false;
        pkiSecretEngine.updateRole("test", roleOptions);

        // Test CSR signing

        // CSR with "csr.example.com" and CN
        String pemCSR = "-----BEGIN CERTIFICATE REQUEST-----\n" +
                "MIICszCCAZsCAQAwbjELMAkGA1UEBhMCVVMxGDAWBgNVBAMMD2Nzci5leGFtcGxl\n" +
                "LmNvbTERMA8GA1UEBwwITmV3IFlvdXIxETAPBgNVBAoMCFRlc3QgT3JnMQswCQYD\n" +
                "VQQIDAJOWTESMBAGA1UECwwJVGVzdCBVbml0MIIBIjANBgkqhkiG9w0BAQEFAAOC\n" +
                "AQ8AMIIBCgKCAQEAn8G6AIZe/FBzLM7ALBt1Z5CcE64dkjYADVLUlSUBX5aPOycg\n" +
                "oPm5RB0DMdgjRjsGvPRvz0NdpH7KsFYfeOh9ltI/YAiGITaHUlEDGH4fi4ZDSpWX\n" +
                "jg4+DhqP3E4/krl9jpeV8PRRTNlrSh5X9wVWsL5rd1q+g5aBTav36/duyTpfwDkL\n" +
                "MKGcaQVFqy+ChNWrj929EuahO2Sw8WLOGDqOQIYF0QlltdPil9YiumUoWUkYjZP9\n" +
                "iunstT0yX+daEhxgahROAen6r7/rj8hCmNBw4CCAVsGHb8u8Ti+yn0Rov7SQxgd3\n" +
                "1smQBvCXKk3HYnlwHHKORFI8v0q/NsE4PswBfwIDAQABoAAwDQYJKoZIhvcNAQEL\n" +
                "BQADggEBABdPp6/5zTGTY/GxONbmKHByVUH4VPPXAFQMUEOitIJI8SxAmoNnGCW2\n" +
                "VTeefrJsyixyLpgG7w5YXnd1947SOa9IN/1BWIMBVhhetPRSNAF+jM6paFmfAiVM\n" +
                "kGPgpHQ1Tk4aPGVwXL51qZP8xwBUMjG+tx0RzRG1fgVUc2NWWNGlYx223xQuwsEg\n" +
                "C3N5T+3bboAvMKTftaKtc43VOqw75iYnY+rOsvjPgPlBRyNuzRtVDhdvL5OlI8AL\n" +
                "Y0EZ2xRrYf2m+BnAGInOThIHqfFsRE7sdNJemE5jJsB5y/tpH4MQi2DZIJce45bu\n" +
                "VVlwf9Wg4h289zEGKPbz35MPUMoQfec=\n" +
                "-----END CERTIFICATE REQUEST-----\n";

        GenerateCertificateOptions options = new GenerateCertificateOptions();
        options.subjectCommonName = "test.example.com";
        options.subjectAlternativeNames = singletonList("alt.example.com");
        options.ipSubjectAlternativeNames = singletonList("1.2.3.4");
        options.uriSubjectAlternativeNames = singletonList("ex:12345");
        options.otherSubjectAlternativeNames = singletonList("1.3.6.1.4.1.311.20.2.3;UTF8:test");
        options.excludeCommonNameFromSubjectAlternativeNames = true;
        options.timeToLive = "333m";

        SignedCertificate result = pkiSecretEngine.signRequest("test", pemCSR, options);

        assertEquals(DataFormat.PEM, result.certificate.getFormat());
        assertNotNull(result.certificate.getData());
        assertFalse(result.certificate.getData().toString().isEmpty());
        assertDoesNotThrow(() -> result.certificate.getCertificate());

        X509CertificateHolder certificate = (X509CertificateHolder) new PEMParser(
                new StringReader((String) result.certificate.getData()))
                        .readObject();
        X509CertificateHolder issuingCA = (X509CertificateHolder) new PEMParser(
                new StringReader((String) result.issuingCA.getData()))
                        .readObject();

        // Check all subject name component options
        assertEquals("CN=test.example.com", certificate.getSubject().toString());

        // Check subjectAlternativeNames, ipSubjectAlternativeNames, uriSubjectAlternativeNames,
        // otherSubjectAlternativeNames & excludeCommonNameFromSubjectAlternativeNames options
        assertNotNull(certificate.getExtension(subjectAlternativeName));
        GeneralNames generalNames = GeneralNames.getInstance(certificate.getExtension(subjectAlternativeName).getParsedValue());
        List<String> subjectAlternativeNames = Arrays.stream(generalNames.getNames())
                .map(GeneralName::getName)
                .map(ASN1Encodable::toString)
                .collect(toList());
        assertEquals(asList("[1.3.6.1.4.1.311.20.2.3, [0]test]", "alt.example.com", "#01020304", "ex:12345"),
                subjectAlternativeNames);

        // Check timeToLive option
        assertEquals(332, Duration.between(Instant.now().plusSeconds(30), certificate.getNotAfter().toInstant()).toMinutes());

        // Check returned cert is not self-signed
        assertNotEquals(certificate.getSubject(), issuingCA.getSubject());

        // Check returned a serial number
        assertNotNull(result.serialNumber);
        assertFalse(result.serialNumber.isEmpty());
    }

    @Test
    public void testGetCertificate() {
        // Generate root
        GenerateRootOptions generateRootOptions = new GenerateRootOptions();
        generateRootOptions.subjectCommonName = "root.example.com";
        generateRootOptions.timeToLive = "24h";
        assertNotNull(pkiSecretEngine.generateRoot(generateRootOptions).certificate);

        // Generate role
        RoleOptions roleOptions = new RoleOptions();
        roleOptions.allowedDomains = singletonList("test.example.com");
        roleOptions.allowBareDomains = true;
        pkiSecretEngine.updateRole("test", roleOptions);

        // Generate cert
        GenerateCertificateOptions options = new GenerateCertificateOptions();
        options.subjectCommonName = "test.example.com";
        String certSerialNumber = pkiSecretEngine.generateCertificate("test", options).serialNumber;

        // Test get cert
        CertificateData.PEM pemCert = pkiSecretEngine.getCertificate(certSerialNumber);
        assertNotNull(pemCert);

        assertEquals(DataFormat.PEM, pemCert.getFormat());
        assertNotNull(pemCert.getData());
        assertFalse(pemCert.getData().isEmpty());
        assertDoesNotThrow(() -> new PEMParser(new StringReader(pemCert.getData())).readObject());
        assertDoesNotThrow(pemCert::getCertificate);
    }

    @Test
    public void testListCertificates() {
        // Generate root
        GenerateRootOptions generateRootOptions = new GenerateRootOptions();
        generateRootOptions.subjectCommonName = "root.example.com";
        generateRootOptions.timeToLive = "24h";
        assertNotNull(pkiSecretEngine.generateRoot(generateRootOptions).certificate);

        // Generate role
        RoleOptions roleOptions = new RoleOptions();
        roleOptions.allowedDomains = singletonList("example.com");
        roleOptions.allowSubdomains = true;
        pkiSecretEngine.updateRole("test", roleOptions);

        // Generate certs
        GenerateCertificateOptions options = new GenerateCertificateOptions();
        options.subjectCommonName = "test1.example.com";
        String certSerialNumber1 = pkiSecretEngine.generateCertificate("test", options).serialNumber;
        options.subjectCommonName = "test2.example.com";
        String certSerialNumber2 = pkiSecretEngine.generateCertificate("test", options).serialNumber;

        // Test list certs
        List<String> certSerials = pkiSecretEngine.getCertificates();
        assertTrue(certSerials.containsAll(asList(certSerialNumber1, certSerialNumber2)));
    }

    @Test
    public void testRevokeCertificate() {
        // Generate root
        GenerateRootOptions generateRootOptions = new GenerateRootOptions();
        generateRootOptions.subjectCommonName = "root.example.com";
        generateRootOptions.timeToLive = "24h";
        assertNotNull(pkiSecretEngine.generateRoot(generateRootOptions).certificate);

        // Generate role
        RoleOptions roleOptions = new RoleOptions();
        roleOptions.allowedDomains = singletonList("test.example.com");
        roleOptions.allowBareDomains = true;
        pkiSecretEngine.updateRole("test", roleOptions);

        // Generate cert
        GenerateCertificateOptions options = new GenerateCertificateOptions();
        options.subjectCommonName = "test.example.com";
        String certSerialNumber = pkiSecretEngine.generateCertificate("test", options).serialNumber;

        // Test revoke
        OffsetDateTime revokedAt = pkiSecretEngine.revokeCertificate(certSerialNumber);
        assertEquals(Duration.between(revokedAt, OffsetDateTime.now()).toMinutes(), 0);
    }

    @Test
    public void testGetCRL() {
        // Generate root
        GenerateRootOptions generateRootOptions = new GenerateRootOptions();
        generateRootOptions.subjectCommonName = "root.example.com";
        generateRootOptions.timeToLive = "24h";
        assertNotNull(pkiSecretEngine.generateRoot(generateRootOptions).certificate);

        // Generate role
        RoleOptions roleOptions = new RoleOptions();
        roleOptions.allowedDomains = singletonList("test.example.com");
        roleOptions.allowBareDomains = true;
        pkiSecretEngine.updateRole("test", roleOptions);

        // Generate cert
        GenerateCertificateOptions options = new GenerateCertificateOptions();
        options.subjectCommonName = "test.example.com";
        String certSerialNumber = pkiSecretEngine.generateCertificate("test", options).serialNumber;

        // Revoke cert
        pkiSecretEngine.revokeCertificate(certSerialNumber);

        // Test CRL get (PEM)
        CRLData.PEM pemCRL = pkiSecretEngine.getCertificateRevocationList();
        assertDoesNotThrow(pemCRL::getCRL);

        // Test CRL get (DER)
        CRLData.DER derCRL = (CRLData.DER) pkiSecretEngine.getCertificateRevocationList(DataFormat.DER);
        assertDoesNotThrow(derCRL::getCRL);
    }

    @Test
    public void testRotateCRL() {
        // Generate root
        GenerateRootOptions generateRootOptions = new GenerateRootOptions();
        generateRootOptions.subjectCommonName = "root.example.com";
        generateRootOptions.timeToLive = "24h";
        assertNotNull(pkiSecretEngine.generateRoot(generateRootOptions).certificate);

        // Test CRL rotate
        assertTrue(pkiSecretEngine.rotateCertificateRevocationList());
    }

    @Test
    public void testGetCAChain() throws Exception {
        // Generate root CA in "pki"
        GenerateRootOptions genRootOptions = new GenerateRootOptions();
        genRootOptions.subjectCommonName = "root.example.com";
        GeneratedRootCertificate generatedRootCertificate = pkiSecretEngine.generateRoot(genRootOptions);
        assertNotNull(generatedRootCertificate.certificate);

        // Generate intermediate CA CSR in "pki2"
        VaultPKISecretEngine pkiSecretEngine2 = pkiSecretEngineFactory.engine("pki2");

        GenerateIntermediateCSROptions genIntCSROptions = new GenerateIntermediateCSROptions();
        genIntCSROptions.subjectCommonName = "test1.example.com";

        GeneratedIntermediateCSRResult csrResult = pkiSecretEngine2.generateIntermediateCSR(genIntCSROptions);
        assertNotNull(csrResult.csr);

        // Sign the intermediate CA using "pki"
        SignIntermediateCAOptions options = new SignIntermediateCAOptions();
        SignedCertificate result = pkiSecretEngine.signIntermediateCA((String) csrResult.csr.getData(), options);

        // Set signed intermediate CA & root CA chain into "pki2"
        pkiSecretEngine2
                .setSignedIntermediateCA(result.certificate.getData() + "\n" + generatedRootCertificate.certificate.getData());

        // Get CA chain and check subjects
        CAChainData.PEM caChainData = pkiSecretEngine2.getCertificateAuthorityChain();
        List<X509Certificate> certificates = assertDoesNotThrow(caChainData::getCertificates);

        assertEquals("CN=root.example.com", certificates.get(1).getSubjectX500Principal().toString());
        assertEquals("CN=test1.example.com", certificates.get(0).getSubjectX500Principal().toString());
    }

    @Test
    public void testConfigureCA() throws Exception {
        // Generate root CA in "pki"
        GenerateRootOptions genRootOptions = new GenerateRootOptions();
        genRootOptions.subjectCommonName = "root.example.com";
        genRootOptions.exportPrivateKey = true;
        GeneratedRootCertificate generatedRootCertificate = pkiSecretEngine.generateRoot(genRootOptions);

        assertNotNull(generatedRootCertificate.certificate);
        assertEquals(DataFormat.PEM, generatedRootCertificate.certificate.getFormat());
        assertNotNull(generatedRootCertificate.certificate.getData());
        assertFalse(generatedRootCertificate.certificate.getData().toString().isEmpty());
        assertDoesNotThrow(() -> generatedRootCertificate.certificate.getCertificate());

        assertNotNull(generatedRootCertificate.issuingCA);
        assertEquals(DataFormat.PEM, generatedRootCertificate.issuingCA.getFormat());
        assertNotNull(generatedRootCertificate.issuingCA.getData());
        assertFalse(generatedRootCertificate.issuingCA.getData().toString().isEmpty());
        assertDoesNotThrow(() -> generatedRootCertificate.issuingCA.getCertificate());

        assertNotNull(generatedRootCertificate.privateKey);
        assertEquals(DataFormat.PEM, generatedRootCertificate.privateKey.getFormat());
        assertNotNull(generatedRootCertificate.privateKey.getData());
        assertFalse(generatedRootCertificate.privateKey.getData().toString().isEmpty());
        assertDoesNotThrow(() -> generatedRootCertificate.privateKey.getKeySpec());

        // Set root CA from "pki" into "pki2"
        VaultPKISecretEngine pkiSecretEngine2 = pkiSecretEngineFactory.engine("pki2");
        pkiSecretEngine2
                .configCertificateAuthority(
                        generatedRootCertificate.certificate.getData() + "\n" + generatedRootCertificate.privateKey.getData());

        // Get CA cert and check subject (PEM)
        CertificateData.PEM pemCAData = pkiSecretEngine2.getCertificateAuthority();
        assertEquals("CN=root.example.com", pemCAData.getCertificate().getSubjectX500Principal().toString());

        // Get CA cert and check subject (DER)
        CertificateData.DER derCAData = (CertificateData.DER) pkiSecretEngine2.getCertificateAuthority(DataFormat.DER);
        assertEquals("CN=root.example.com", derCAData.getCertificate().getSubjectX500Principal().toString());
    }

    @Test
    public void testConfigureURLs() {
        // Generate root
        GenerateRootOptions generateRootOptions = new GenerateRootOptions();
        generateRootOptions.subjectCommonName = "root.example.com";
        generateRootOptions.timeToLive = "24h";
        assertNotNull(pkiSecretEngine.generateRoot(generateRootOptions).certificate);

        // Update URLs & read them
        ConfigURLsOptions options = new ConfigURLsOptions();
        options.issuingCertificates = asList("certs1.example.com", "certs2.example.com");
        options.crlDistributionPoints = asList("crl1.example.com", "crl2.example.com");
        options.ocspServers = asList("ocsp1.example.com", "ocsp2.example.com");

        pkiSecretEngine.configURLs(options);

        ConfigURLsOptions result = pkiSecretEngine.readURLsConfig();
        assertEquals(options.issuingCertificates, result.issuingCertificates);
        assertEquals(options.crlDistributionPoints, result.crlDistributionPoints);
        assertEquals(options.ocspServers, result.ocspServers);
    }

    @Test
    public void testConfigureCRL() {
        // Generate root
        GenerateRootOptions generateRootOptions = new GenerateRootOptions();
        generateRootOptions.subjectCommonName = "root.example.com";
        generateRootOptions.timeToLive = "24h";
        assertNotNull(pkiSecretEngine.generateRoot(generateRootOptions).certificate);

        // Update CRL & read them
        ConfigCRLOptions options = new ConfigCRLOptions();
        options.expiry = "123h";
        options.disable = true;

        pkiSecretEngine.configCRL(options);

        ConfigCRLOptions result = pkiSecretEngine.readCRLConfig();
        assertEquals(options.expiry, result.expiry);
        assertEquals(options.disable, result.disable);
    }

    @Test
    public void testTidy() {
        // Generate root
        GenerateRootOptions generateRootOptions = new GenerateRootOptions();
        generateRootOptions.subjectCommonName = "root.example.com";
        generateRootOptions.timeToLive = "24h";
        assertNotNull(pkiSecretEngine.generateRoot(generateRootOptions).certificate);

        // Execute tidy
        TidyOptions options = new TidyOptions();
        options.tidyCertStore = true;
        options.tidyRevokedCerts = true;
        options.safetyBuffer = "24h";
        assertDoesNotThrow(() -> pkiSecretEngine.tidy(options));
    }
}
