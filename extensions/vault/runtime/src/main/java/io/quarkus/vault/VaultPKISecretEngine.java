package io.quarkus.vault;

import java.time.OffsetDateTime;
import java.util.List;

import io.quarkus.vault.pki.ConfigCRLOptions;
import io.quarkus.vault.pki.ConfigURLsOptions;
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

/**
 * A service that interacts with Hashicorp's Vault PKI secret engine to issue certificates & manage certificate
 * authorities.
 *
 * @see <a href="https://www.vaultproject.io/docs/secrets/pki">PKI</a>
 */
public interface VaultPKISecretEngine {

    /**
     * Retrieves the engine's PEM encoded CA certificate.
     * 
     * @return PEM encoded certificate authority certificate.
     */
    String getCertificateAuthority();

    /**
     * Configures the engine's CA.
     *
     * @param pemBundle PEM encoded bundle including the CA, with optional chain, and private key.
     */
    void configCertificateAuthority(String pemBundle);

    /**
     * Configures engine's URLs for issuing certificates, CRL distribution points, and OCSP servers.
     *
     * @param options URL options.
     */
    void configURLs(ConfigURLsOptions options);

    /**
     * Read engine's configured URLs for issuing certificates, CRL distribution points, and OCSP servers.
     *
     * @return URL options.
     */
    ConfigURLsOptions readURLsConfig();

    /**
     * Configures engine's CRL.
     *
     * @param options CRL options.
     */
    void configCRL(ConfigCRLOptions options);

    /**
     * Read engine's CRL configuration.
     *
     * @return URL options.
     */
    ConfigCRLOptions readCRLConfig();

    /**
     * Retrieves the engine's PEM encoded CA chain.
     * 
     * @return PEM encoded certificate authority chain.
     */
    String getCertificateAuthorityChain();

    /**
     * Retrieves the engine's PEM encoded CRL.
     * 
     * @return PEM encoded certificate revocation list.
     */
    String getCertificateRevocationList();

    /**
     * Forces a rotation of the associated CRL.
     */
    boolean rotateCertificateRevocationList();

    /**
     * List all issued certificate serial numbers.
     * 
     * @return List of certificate serialize numbers.
     */
    List<String> getCertificates();

    /**
     * Retrieve a specific PEM encoded certificate.
     * 
     * @param serial Serial number of certificate.
     * @return PEM encoded certificate or null if no certificate exists.
     */
    String getCertificate(String serial);

    /**
     * Generates a public/private key pair and certificate issued from the engine's CA using the
     * provided options.
     *
     * @param role Name of role used to create certificate.
     * @param options Certificate generation options.
     * @return Generated certificate and private key.
     */
    GeneratedCertificate generateCertificate(String role, GenerateCertificateOptions options);

    /**
     * Generates a certificate issued from the engine's CA using the provided Certificate Signing Request and options.
     *
     * @param role Name of role used to create certificate.
     * @param pemSigningRequest Certificate Signing Request (PEM encoded).
     * @param options Certificate generation options.
     * @return Generated certificate.
     */
    SignedCertificate signRequest(String role, String pemSigningRequest, GenerateCertificateOptions options);

    /**
     * Revokes a certificate.
     *
     * @param serialNumber Serial number of certificate.
     * @return Time of certificates revocation.
     */
    OffsetDateTime revokeCertificate(String serialNumber);

    /**
     * Updates, or creates, a role.
     *
     * @param role Name of role.
     * @param options Options for role.
     */
    void updateRole(String role, RoleOptions options);

    /**
     * Retrieve current options for a role.
     *
     * @param role Name of role.
     * @return Options for the role or null if role does not exist.
     */
    RoleOptions getRole(String role);

    /**
     * Lists existing role names.
     *
     * @return List of role names.
     */
    List<String> getRoles();

    /**
     * Deletes a role.
     *
     * @param role Name of role.
     */
    void deleteRole(String role);

    /**
     * Generates a self-signed root as the engine's CA.
     *
     * @param options Generation options.
     * @return Generated root certificate.
     */
    GeneratedRootCertificate generateRoot(GenerateRootOptions options);

    /**
     * Deletes the engine's current CA.
     */
    void deleteRoot();

    /**
     * Generates an intermediate CA certificate issued from the engine's CA using the provided Certificate Signing
     * Request and options.
     *
     * @param pemSigningRequest Certificate Signing Request (PEM encoded).
     * @param options Signing options.
     * @return Generated certificate.
     */
    SignedCertificate signIntermediateCA(String pemSigningRequest, SignIntermediateCAOptions options);

    /**
     * Generates a Certificate Signing Request and private key for the engine's CA.
     *
     * Use this to generate a CSR and for the engine's CA that can be used by another
     * CA to issue an intermediate CA certificate. After generating the intermediate CA
     * {@link #setSignedIntermediateCA(String)} must be used to set the engine's CA certificate.
     *
     * This will overwrite any previously existing CA private key for the engine.
     *
     * @see #setSignedIntermediateCA(String)
     * @param options Options for CSR generation.
     * @return Generated CSR and, if key export is enabled, private key.
     */
    GeneratedIntermediateCSRResult generateIntermediateCSR(GenerateIntermediateCSROptions options);

    /**
     * Sets the engine's intermediate CA certificate, signed by another CA.
     *
     * After generating a CSR (via {@link #generateIntermediateCSR(GenerateIntermediateCSROptions)}),
     * this method must be used to set the engine's CA.
     *
     * @see #generateIntermediateCSR(GenerateIntermediateCSROptions)
     * @param pemCert Signed certificate (PEM encoded).
     */
    void setSignedIntermediateCA(String pemCert);

    /**
     * Tidy up the storage backend and/or CRL by removing certificates that have expired and are past a certain buffer
     * period beyond their expiration time.
     *
     * @param options Tidy options.
     */
    void tidy(TidyOptions options);

}
