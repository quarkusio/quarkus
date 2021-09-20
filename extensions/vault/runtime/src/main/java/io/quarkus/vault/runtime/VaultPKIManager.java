package io.quarkus.vault.runtime;

import static io.quarkus.vault.runtime.VaultPKIManagerFactory.PKI_ENGINE_NAME;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.vault.VaultException;
import io.quarkus.vault.VaultPKISecretEngine;
import io.quarkus.vault.pki.CAChainData;
import io.quarkus.vault.pki.CRLData;
import io.quarkus.vault.pki.CSRData;
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
import io.quarkus.vault.pki.PrivateKeyData;
import io.quarkus.vault.pki.PrivateKeyEncoding;
import io.quarkus.vault.pki.RoleOptions;
import io.quarkus.vault.pki.SignIntermediateCAOptions;
import io.quarkus.vault.pki.SignedCertificate;
import io.quarkus.vault.pki.TidyOptions;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.client.dto.AbstractVaultDTO;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKICRLRotateResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKICertificateListResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKICertificateResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIConfigCABody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIConfigCRLData;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIConfigCRLResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIConfigURLsData;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIConfigURLsResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIConstants;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIGenerateCertificateBody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIGenerateCertificateData;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIGenerateCertificateResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIGenerateIntermediateCSRBody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIGenerateIntermediateCSRData;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIGenerateIntermediateCSRResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIGenerateRootBody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIGenerateRootData;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIGenerateRootResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIRevokeCertificateBody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIRevokeCertificateResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIRoleOptionsData;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIRoleReadResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIRolesListResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKISetSignedIntermediateCABody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKISignCertificateRequestBody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKISignCertificateRequestData;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKISignCertificateRequestResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKISignIntermediateCABody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKITidyBody;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalPKISecretEngine;
import io.vertx.mutiny.core.buffer.Buffer;

@ApplicationScoped
public class VaultPKIManager implements VaultPKISecretEngine {

    private final String mount;
    private final VaultAuthManager vaultAuthManager;
    private final VaultInternalPKISecretEngine vaultInternalPKISecretEngine;

    @Inject
    public VaultPKIManager(
            VaultAuthManager vaultAuthManager,
            VaultInternalPKISecretEngine vaultInternalPKISecretEngine) {
        this(PKI_ENGINE_NAME, vaultAuthManager, vaultInternalPKISecretEngine);
    }

    VaultPKIManager(
            String mount,
            VaultAuthManager vaultAuthManager,
            VaultInternalPKISecretEngine vaultInternalPKISecretEngine) {
        this.mount = mount;
        this.vaultAuthManager = vaultAuthManager;
        this.vaultInternalPKISecretEngine = vaultInternalPKISecretEngine;
    }

    private String getToken() {
        return vaultAuthManager.getClientToken();
    }

    @Override
    public CertificateData.PEM getCertificateAuthority() {
        return (CertificateData.PEM) getCertificateAuthority(DataFormat.PEM);
    }

    @Override
    public CertificateData getCertificateAuthority(DataFormat format) {
        String vaultFormat = format == DataFormat.PEM ? format.name().toLowerCase(Locale.ROOT) : null;
        Buffer data = vaultInternalPKISecretEngine.getCertificateAuthority(getToken(), mount, vaultFormat);

        switch (format) {
            case PEM:
                return new CertificateData.PEM(data.toString(StandardCharsets.UTF_8));
            case DER:
                return new CertificateData.DER(data.getBytes());
            default:
                throw new VaultException("Unsupported Data Format");
        }
    }

    @Override
    public void configCertificateAuthority(String pemBundle) {
        VaultPKIConfigCABody body = new VaultPKIConfigCABody();
        body.pemBundle = pemBundle;
        vaultInternalPKISecretEngine.configCertificateAuthority(getToken(), mount, body);
    }

    @Override
    public void configURLs(ConfigURLsOptions options) {
        VaultPKIConfigURLsData body = new VaultPKIConfigURLsData();
        body.issuingCertificates = options.issuingCertificates;
        body.crlDistributionPoints = options.crlDistributionPoints;
        body.ocspServers = options.ocspServers;

        vaultInternalPKISecretEngine.configURLs(getToken(), mount, body);
    }

    @Override
    public ConfigURLsOptions readURLsConfig() {
        VaultPKIConfigURLsResult internalResult = vaultInternalPKISecretEngine.readURLs(getToken(), mount);
        checkDataValid(internalResult);

        VaultPKIConfigURLsData internalResultData = internalResult.data;

        ConfigURLsOptions result = new ConfigURLsOptions();
        result.issuingCertificates = internalResultData.issuingCertificates;
        result.crlDistributionPoints = internalResultData.crlDistributionPoints;
        result.ocspServers = internalResultData.ocspServers;
        return result;
    }

    @Override
    public void configCRL(ConfigCRLOptions options) {
        VaultPKIConfigCRLData body = new VaultPKIConfigCRLData();
        body.expiry = options.expiry;
        body.disable = options.disable;

        vaultInternalPKISecretEngine.configCRL(getToken(), mount, body);
    }

    @Override
    public ConfigCRLOptions readCRLConfig() {
        VaultPKIConfigCRLResult internalResult = vaultInternalPKISecretEngine.readCRL(getToken(), mount);
        checkDataValid(internalResult);

        VaultPKIConfigCRLData internalResultData = internalResult.data;

        ConfigCRLOptions result = new ConfigCRLOptions();
        result.expiry = internalResultData.expiry;
        result.disable = internalResultData.disable;
        return result;
    }

    @Override
    public CAChainData.PEM getCertificateAuthorityChain() {
        Buffer data = vaultInternalPKISecretEngine.getCertificateAuthorityChain(getToken(), mount);
        return new CAChainData.PEM(data.toString(StandardCharsets.UTF_8));
    }

    @Override
    public CRLData.PEM getCertificateRevocationList() {
        return (CRLData.PEM) getCertificateRevocationList(DataFormat.PEM);
    }

    @Override
    public CRLData getCertificateRevocationList(DataFormat format) {
        String vaultFormat = format == DataFormat.PEM ? format.name().toLowerCase(Locale.ROOT) : null;
        Buffer data = vaultInternalPKISecretEngine.getCertificateRevocationList(getToken(), mount, vaultFormat);

        switch (format) {
            case PEM:
                return new CRLData.PEM(data.toString(StandardCharsets.UTF_8));
            case DER:
                return new CRLData.DER(data.getBytes());
            default:
                throw new VaultException("Unsupported Data Format");
        }
    }

    @Override
    public boolean rotateCertificateRevocationList() {
        VaultPKICRLRotateResult internalResult = vaultInternalPKISecretEngine.rotateCertificateRevocationList(getToken(),
                mount);
        checkDataValid(internalResult);

        return internalResult.data.success;
    }

    @Override
    public List<String> getCertificates() {
        VaultPKICertificateListResult internalResult = vaultInternalPKISecretEngine.listCertificates(getToken(), mount);
        checkDataValid(internalResult);

        // Return serials corrected to colon format (to match those returned by generateCertificate/signRequest)
        return internalResult.data.keys.stream().map(serial -> serial.replaceAll("-", ":")).collect(toList());
    }

    @Override
    public CertificateData.PEM getCertificate(String serial) {
        VaultPKICertificateResult internalResult = vaultInternalPKISecretEngine.getCertificate(getToken(), mount, serial);
        checkDataValid(internalResult);

        return new CertificateData.PEM(internalResult.data.certificate);
    }

    @Override
    public GeneratedCertificate generateCertificate(String role, GenerateCertificateOptions options) {
        VaultPKIGenerateCertificateBody body = new VaultPKIGenerateCertificateBody();
        body.format = dataFormatToFormat(options.format);
        body.privateKeyFormat = privateKeyFormat(options.format, options.privateKeyEncoding);
        body.subjectCommonName = options.subjectCommonName;
        body.subjectAlternativeNames = stringListToCommaString(options.subjectAlternativeNames);
        body.ipSubjectAlternativeNames = stringListToCommaString(options.ipSubjectAlternativeNames);
        body.uriSubjectAlternativeNames = stringListToCommaString(options.uriSubjectAlternativeNames);
        body.otherSubjectAlternativeNames = options.otherSubjectAlternativeNames;
        body.timeToLive = options.timeToLive;
        body.excludeCommonNameFromSubjectAlternativeNames = options.excludeCommonNameFromSubjectAlternativeNames;

        VaultPKIGenerateCertificateResult internalResult = vaultInternalPKISecretEngine.generateCertificate(getToken(), mount,
                role, body);
        checkDataValid(internalResult);

        VaultPKIGenerateCertificateData internalResultData = internalResult.data;

        GeneratedCertificate result = new GeneratedCertificate();
        result.certificate = createCertificateData(internalResultData.certificate, body.format);
        result.issuingCA = createCertificateData(internalResultData.issuingCA, body.format);
        result.caChain = createCertificateDataList(internalResultData.caChain, body.format);
        result.serialNumber = internalResultData.serialNumber;
        result.privateKeyType = stringToCertificateKeyType(internalResultData.privateKeyType);
        result.privateKey = createPrivateKeyData(internalResultData.privateKey, body.format, body.privateKeyFormat);
        return result;
    }

    @Override
    public SignedCertificate signRequest(String role, String pemSigningRequest, GenerateCertificateOptions options) {
        VaultPKISignCertificateRequestBody body = new VaultPKISignCertificateRequestBody();
        body.format = dataFormatToFormat(options.format);
        body.csr = pemSigningRequest;
        body.subjectCommonName = options.subjectCommonName;
        body.subjectAlternativeNames = stringListToCommaString(options.subjectAlternativeNames);
        body.ipSubjectAlternativeNames = stringListToCommaString(options.ipSubjectAlternativeNames);
        body.uriSubjectAlternativeNames = stringListToCommaString(options.uriSubjectAlternativeNames);
        body.otherSubjectAlternativeNames = options.otherSubjectAlternativeNames;
        body.timeToLive = options.timeToLive;
        body.excludeCommonNameFromSubjectAlternativeNames = options.excludeCommonNameFromSubjectAlternativeNames;

        VaultPKISignCertificateRequestResult internalResult = vaultInternalPKISecretEngine.signCertificate(getToken(), mount,
                role, body);
        checkDataValid(internalResult);

        VaultPKISignCertificateRequestData internalResultData = internalResult.data;

        SignedCertificate result = new SignedCertificate();
        result.certificate = createCertificateData(internalResultData.certificate, body.format);
        result.issuingCA = createCertificateData(internalResultData.issuingCA, body.format);
        result.caChain = createCertificateDataList(internalResultData.caChain, body.format);
        result.serialNumber = internalResultData.serialNumber;
        return result;
    }

    @Override
    public OffsetDateTime revokeCertificate(String serialNumber) {
        VaultPKIRevokeCertificateBody body = new VaultPKIRevokeCertificateBody();
        body.serialNumber = serialNumber;

        VaultPKIRevokeCertificateResult internalResult = vaultInternalPKISecretEngine.revokeCertificate(getToken(), mount,
                body);
        checkDataValid(internalResult);

        return internalResult.data.revocationTime;
    }

    @Override
    public void updateRole(String role, RoleOptions options) {
        VaultPKIRoleOptionsData body = new VaultPKIRoleOptionsData();
        body.timeToLive = options.timeToLive;
        body.maxTimeToLive = options.maxTimeToLive;
        body.allowLocalhost = options.allowLocalhost;
        body.allowedDomains = options.allowedDomains;
        body.allowTemplatesInAllowedDomains = options.allowTemplatesInAllowedDomains;
        body.allowBareDomains = options.allowBareDomains;
        body.allowSubdomains = options.allowSubdomains;
        body.allowGlobsInAllowedDomains = options.allowGlobsInAllowedDomains;
        body.allowAnyName = options.allowAnyName;
        body.enforceHostnames = options.enforceHostnames;
        body.allowIpSubjectAlternativeNames = options.allowIpSubjectAlternativeNames;
        body.allowedUriSubjectAlternativeNames = options.allowedUriSubjectAlternativeNames;
        body.allowedOtherSubjectAlternativeNames = options.allowedOtherSubjectAlternativeNames;
        body.serverFlag = options.serverFlag;
        body.clientFlag = options.clientFlag;
        body.codeSigningFlag = options.codeSigningFlag;
        body.emailProtectionFlag = options.emailProtectionFlag;
        body.keyType = certificateKeyTypeToString(options.keyType);
        body.keyBits = options.keyBits;
        body.keyUsages = enumListToStringList(options.keyUsages, CertificateKeyUsage::name);
        body.extendedKeyUsages = enumListToStringList(options.extendedKeyUsages, CertificateExtendedKeyUsage::name);
        body.extendedKeyUsageOIDs = options.extendedKeyUsageOIDs;
        body.useCSRCommonName = options.useCSRCommonName;
        body.useCSRSubjectAlternativeNames = options.useCSRSubjectAlternativeNames;
        body.subjectOrganization = commaStringToStringList(options.subjectOrganization);
        body.subjectOrganizationalUnit = commaStringToStringList(options.subjectOrganizationalUnit);
        body.subjectStreetAddress = commaStringToStringList(options.subjectStreetAddress);
        body.subjectPostalCode = commaStringToStringList(options.subjectPostalCode);
        body.subjectLocality = commaStringToStringList(options.subjectLocality);
        body.subjectProvince = commaStringToStringList(options.subjectProvince);
        body.subjectCountry = commaStringToStringList(options.subjectCountry);
        body.allowedSubjectSerialNumbers = options.allowedSubjectSerialNumbers;
        body.generateLease = options.generateLease;
        body.noStore = options.noStore;
        body.requireCommonName = options.requireCommonName;
        body.policyOIDs = options.policyOIDs;
        body.basicConstraintsValidForNonCA = options.basicConstraintsValidForNonCA;
        body.notBeforeDuration = options.notBeforeDuration;

        vaultInternalPKISecretEngine.updateRole(getToken(), mount, role, body);
    }

    @Override
    public RoleOptions getRole(String role) {
        VaultPKIRoleReadResult internalResult = vaultInternalPKISecretEngine.readRole(getToken(), mount, role);
        checkDataValid(internalResult);

        VaultPKIRoleOptionsData internalResultData = internalResult.data;

        RoleOptions result = new RoleOptions();
        result.timeToLive = internalResultData.timeToLive;
        result.maxTimeToLive = internalResultData.maxTimeToLive;
        result.allowLocalhost = internalResultData.allowLocalhost;
        result.allowedDomains = internalResultData.allowedDomains;
        result.allowTemplatesInAllowedDomains = internalResultData.allowTemplatesInAllowedDomains;
        result.allowBareDomains = internalResultData.allowBareDomains;
        result.allowSubdomains = internalResultData.allowSubdomains;
        result.allowGlobsInAllowedDomains = internalResultData.allowGlobsInAllowedDomains;
        result.allowAnyName = internalResultData.allowAnyName;
        result.enforceHostnames = internalResultData.enforceHostnames;
        result.allowIpSubjectAlternativeNames = internalResultData.allowIpSubjectAlternativeNames;
        result.allowedUriSubjectAlternativeNames = internalResultData.allowedUriSubjectAlternativeNames;
        result.allowedOtherSubjectAlternativeNames = internalResultData.allowedOtherSubjectAlternativeNames;
        result.serverFlag = internalResultData.serverFlag;
        result.clientFlag = internalResultData.clientFlag;
        result.codeSigningFlag = internalResultData.codeSigningFlag;
        result.emailProtectionFlag = internalResultData.emailProtectionFlag;
        result.keyType = stringToCertificateKeyType(internalResultData.keyType);
        result.keyBits = internalResultData.keyBits;
        result.keyUsages = stringListToEnumList(internalResultData.keyUsages, CertificateKeyUsage::valueOf);
        result.extendedKeyUsages = stringListToEnumList(internalResultData.extendedKeyUsages,
                CertificateExtendedKeyUsage::valueOf);
        result.extendedKeyUsageOIDs = internalResultData.extendedKeyUsageOIDs;
        result.useCSRCommonName = internalResultData.useCSRCommonName;
        result.useCSRSubjectAlternativeNames = internalResultData.useCSRSubjectAlternativeNames;
        result.subjectOrganization = stringListToCommaString(internalResultData.subjectOrganization);
        result.subjectOrganizationalUnit = stringListToCommaString(internalResultData.subjectOrganizationalUnit);
        result.subjectStreetAddress = stringListToCommaString(internalResultData.subjectStreetAddress);
        result.subjectPostalCode = stringListToCommaString(internalResultData.subjectPostalCode);
        result.subjectLocality = stringListToCommaString(internalResultData.subjectLocality);
        result.subjectProvince = stringListToCommaString(internalResultData.subjectProvince);
        result.subjectCountry = stringListToCommaString(internalResultData.subjectCountry);
        result.allowedSubjectSerialNumbers = internalResultData.allowedSubjectSerialNumbers;
        result.generateLease = internalResultData.generateLease;
        result.noStore = internalResultData.noStore;
        result.requireCommonName = internalResultData.requireCommonName;
        result.policyOIDs = internalResultData.policyOIDs;
        result.basicConstraintsValidForNonCA = internalResultData.basicConstraintsValidForNonCA;
        result.notBeforeDuration = internalResultData.notBeforeDuration;
        return result;
    }

    @Override
    public List<String> getRoles() {
        try {

            VaultPKIRolesListResult internalResult = vaultInternalPKISecretEngine.listRoles(getToken(), mount);
            checkDataValid(internalResult);

            return internalResult.data.keys;

        } catch (VaultClientException x) {
            // Translate 404 to empty list
            if (x.getStatus() == 404) {
                return emptyList();
            } else {
                throw x;
            }
        }
    }

    @Override
    public void deleteRole(String role) {
        vaultInternalPKISecretEngine.deleteRole(getToken(), mount, role);
    }

    @Override
    public GeneratedRootCertificate generateRoot(GenerateRootOptions options) {
        String type = options.exportPrivateKey ? "exported" : "internal";
        VaultPKIGenerateRootBody body = new VaultPKIGenerateRootBody();
        body.format = dataFormatToFormat(options.format);
        body.privateKeyFormat = privateKeyFormat(options.format, options.privateKeyEncoding);
        body.subjectCommonName = options.subjectCommonName;
        body.subjectAlternativeNames = stringListToCommaString(options.subjectAlternativeNames);
        body.ipSubjectAlternativeNames = stringListToCommaString(options.ipSubjectAlternativeNames);
        body.uriSubjectAlternativeNames = stringListToCommaString(options.uriSubjectAlternativeNames);
        body.otherSubjectAlternativeNames = options.otherSubjectAlternativeNames;
        body.timeToLive = options.timeToLive;
        body.keyType = certificateKeyTypeToString(options.keyType);
        body.keyBits = options.keyBits;
        body.maxPathLength = options.maxPathLength;
        body.excludeCommonNameFromSubjectAlternativeNames = options.excludeCommonNameFromSubjectAlternativeNames;
        body.permittedDnsDomains = options.permittedDnsDomains;
        body.subjectOrganization = options.subjectOrganization;
        body.subjectOrganizationalUnit = options.subjectOrganizationalUnit;
        body.subjectStreetAddress = options.subjectStreetAddress;
        body.subjectPostalCode = options.subjectPostalCode;
        body.subjectLocality = options.subjectLocality;
        body.subjectProvince = options.subjectProvince;
        body.subjectCountry = options.subjectCountry;
        body.subjectSerialNumber = options.subjectSerialNumber;

        VaultPKIGenerateRootResult internalResult = vaultInternalPKISecretEngine.generateRoot(getToken(), mount, type, body);
        checkDataValid(internalResult);

        VaultPKIGenerateRootData internalResultData = internalResult.data;

        GeneratedRootCertificate result = new GeneratedRootCertificate();
        result.certificate = createCertificateData(internalResultData.certificate, body.format);
        result.issuingCA = createCertificateData(internalResultData.issuingCA, body.format);
        result.serialNumber = internalResultData.serialNumber;
        result.privateKeyType = stringToCertificateKeyType(internalResultData.privateKeyType);
        result.privateKey = createPrivateKeyData(internalResultData.privateKey, body.format, body.privateKeyFormat);
        return result;
    }

    @Override
    public void deleteRoot() {
        vaultInternalPKISecretEngine.deleteRoot(getToken(), mount);
    }

    @Override
    public SignedCertificate signIntermediateCA(String pemSigningRequest, SignIntermediateCAOptions options) {
        VaultPKISignIntermediateCABody body = new VaultPKISignIntermediateCABody();
        body.format = dataFormatToFormat(options.format);
        body.csr = pemSigningRequest;
        body.subjectCommonName = options.subjectCommonName;
        body.subjectAlternativeNames = stringListToCommaString(options.subjectAlternativeNames);
        body.ipSubjectAlternativeNames = stringListToCommaString(options.ipSubjectAlternativeNames);
        body.uriSubjectAlternativeNames = stringListToCommaString(options.uriSubjectAlternativeNames);
        body.otherSubjectAlternativeNames = options.otherSubjectAlternativeNames;
        body.timeToLive = options.timeToLive;
        body.maxPathLength = options.maxPathLength;
        body.excludeCommonNameFromSubjectAlternativeNames = options.excludeCommonNameFromSubjectAlternativeNames;
        body.useCSRValues = options.useCSRValues;
        body.permittedDnsDomains = options.permittedDnsDomains;
        body.subjectOrganization = options.subjectOrganization;
        body.subjectOrganizationalUnit = options.subjectOrganizationalUnit;
        body.subjectStreetAddress = options.subjectStreetAddress;
        body.subjectPostalCode = options.subjectPostalCode;
        body.subjectLocality = options.subjectLocality;
        body.subjectProvince = options.subjectProvince;
        body.subjectCountry = options.subjectCountry;
        body.subjectSerialNumber = options.subjectSerialNumber;

        VaultPKISignCertificateRequestResult internalResult = vaultInternalPKISecretEngine.signIntermediateCA(getToken(), mount,
                body);
        checkDataValid(internalResult);

        VaultPKISignCertificateRequestData internalResultData = internalResult.data;

        SignedCertificate result = new SignedCertificate();
        result.certificate = createCertificateData(internalResultData.certificate, body.format);
        result.issuingCA = createCertificateData(internalResultData.issuingCA, body.format);
        result.caChain = createCertificateDataList(internalResultData.caChain, body.format);
        result.serialNumber = internalResultData.serialNumber;
        return result;
    }

    @Override
    public GeneratedIntermediateCSRResult generateIntermediateCSR(GenerateIntermediateCSROptions options) {
        String type = options.exportPrivateKey ? "exported" : "internal";
        VaultPKIGenerateIntermediateCSRBody body = new VaultPKIGenerateIntermediateCSRBody();
        body.format = dataFormatToFormat(options.format);
        body.privateKeyFormat = privateKeyFormat(options.format, options.privateKeyEncoding);
        body.subjectCommonName = options.subjectCommonName;
        body.subjectAlternativeNames = stringListToCommaString(options.subjectAlternativeNames);
        body.ipSubjectAlternativeNames = stringListToCommaString(options.ipSubjectAlternativeNames);
        body.uriSubjectAlternativeNames = stringListToCommaString(options.uriSubjectAlternativeNames);
        body.otherSubjectAlternativeNames = options.otherSubjectAlternativeNames;
        body.keyType = certificateKeyTypeToString(options.keyType);
        body.keyBits = options.keyBits;
        body.excludeCommonNameFromSubjectAlternativeNames = options.excludeCommonNameFromSubjectAlternativeNames;
        body.subjectOrganization = options.subjectOrganization;
        body.subjectOrganizationalUnit = options.subjectOrganizationalUnit;
        body.subjectStreetAddress = options.subjectStreetAddress;
        body.subjectPostalCode = options.subjectPostalCode;
        body.subjectLocality = options.subjectLocality;
        body.subjectProvince = options.subjectProvince;
        body.subjectCountry = options.subjectCountry;
        body.subjectSerialNumber = options.subjectSerialNumber;

        VaultPKIGenerateIntermediateCSRResult internalResult = vaultInternalPKISecretEngine.generateIntermediateCSR(getToken(),
                mount, type, body);

        VaultPKIGenerateIntermediateCSRData internalResultData = internalResult.data;

        GeneratedIntermediateCSRResult result = new GeneratedIntermediateCSRResult();
        result.csr = createCSRData(internalResultData.csr, body.format);
        result.privateKeyType = stringToCertificateKeyType(internalResultData.privateKeyType);
        result.privateKey = createPrivateKeyData(internalResultData.privateKey, body.format, body.privateKeyFormat);
        return result;
    }

    @Override
    public void setSignedIntermediateCA(String pemCert) {
        VaultPKISetSignedIntermediateCABody body = new VaultPKISetSignedIntermediateCABody();
        body.certificate = pemCert;

        vaultInternalPKISecretEngine.setSignedIntermediateCA(getToken(), mount, body);
    }

    @Override
    public void tidy(TidyOptions options) {
        VaultPKITidyBody body = new VaultPKITidyBody();
        body.tidyCertStore = options.tidyCertStore;
        body.tidyRevokedCerts = options.tidyRevokedCerts;
        body.safetyBuffer = options.safetyBuffer;

        vaultInternalPKISecretEngine.tidy(getToken(), mount, body);
    }

    private String stringListToCommaString(List<String> values) {
        if (values == null) {
            return null;
        }
        return String.join(",", values);
    }

    private List<String> commaStringToStringList(String value) {
        if (value == null) {
            return null;
        }
        return asList(value.split(","));
    }

    private CertificateKeyType stringToCertificateKeyType(String value) {
        if (value == null) {
            return null;
        }
        return CertificateKeyType.valueOf(value.toUpperCase());
    }

    private String certificateKeyTypeToString(CertificateKeyType value) {
        if (value == null) {
            return null;
        }
        return value.name().toLowerCase();
    }

    private <T extends Enum<T>> List<String> enumListToStringList(List<T> values, Function<T, String> converter) {
        if (values == null) {
            return null;
        }
        return values.stream().map(converter).collect(toList());
    }

    private <T extends Enum<T>> List<T> stringListToEnumList(List<String> values, Function<String, T> converter) {
        if (values == null) {
            return null;
        }
        return values.stream().map(converter).collect(toList());
    }

    private void checkDataValid(AbstractVaultDTO<?, ?> dto) {
        if (dto.data != null) {
            return;
        }
        if (dto.warnings instanceof List<?>) {
            List<?> warnings = (List<?>) dto.warnings;
            if (!warnings.isEmpty()) {
                throw new VaultException(warnings.get(0).toString());
            }
        }
        throw new VaultException("Unknown vault error");
    }

    private String dataFormatToFormat(DataFormat format) {
        if (format == null) {
            return VaultPKIConstants.DEFAULT_CERTIFICATE_FORMAT;
        }
        return format.name().toLowerCase(Locale.ROOT);
    }

    private String nonNullFormat(String format) {
        if (format == null) {
            return VaultPKIConstants.DEFAULT_CERTIFICATE_FORMAT;
        }
        return format;
    }

    private String privateKeyFormat(DataFormat format, PrivateKeyEncoding privateKeyEncoding) {
        if (privateKeyEncoding == null) {
            return VaultPKIConstants.DEFAULT_KEY_ENCODING;
        }
        if (privateKeyEncoding == PrivateKeyEncoding.PKCS8) {
            return "pkcs8";
        }
        return dataFormatToFormat(format);
    }

    private CertificateData createCertificateData(String data, String format) {
        if (data == null) {
            return null;
        }
        switch (nonNullFormat(format)) {
            case "der":
                return new CertificateData.DER(Base64.getDecoder().decode(data));
            case "pem":
                return new CertificateData.PEM(data);
            default:
                throw new VaultException("Unsupported certificate format");
        }
    }

    private List<CertificateData> createCertificateDataList(List<String> datas, String format) {
        if (datas == null) {
            return null;
        }
        List<CertificateData> result = new ArrayList<>(datas.size());
        for (String data : datas) {
            result.add(createCertificateData(data, format));
        }
        return result;
    }

    private CSRData createCSRData(String data, String format) {
        if (data == null) {
            return null;
        }
        switch (nonNullFormat(format)) {
            case "der":
                return new CSRData.DER(Base64.getDecoder().decode(data));
            case "pem":
                return new CSRData.PEM(data);
            default:
                throw new VaultException("Unsupported certification request format");
        }
    }

    private PrivateKeyData createPrivateKeyData(String data, String format, String privateKeyFormat) {
        if (data == null) {
            return null;
        }
        boolean pkcs8 = "pkcs8".equals(privateKeyFormat.toLowerCase(Locale.ROOT));
        switch (nonNullFormat(format)) {
            case "der":
                return new PrivateKeyData.DER(Base64.getDecoder().decode(data), pkcs8);
            case "pem":
                return new PrivateKeyData.PEM(data, pkcs8);
            default:
                throw new VaultException("Unsupported private key format");
        }
    }
}
