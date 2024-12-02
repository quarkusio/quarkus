package io.quarkus.oidc.runtime;

import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jboss.logging.Logger;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.lang.UnresolvableKeyException;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenCertificateValidator;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.vertx.ext.auth.impl.CertificateHelper;

public class CertChainPublicKeyResolver implements RefreshableVerificationKeyResolver {
    private static final Logger LOG = Logger.getLogger(OidcProvider.class);
    final OidcTenantConfig oidcConfig;
    final Set<String> thumbprints;
    final Optional<String> expectedLeafCertificateName;
    final List<TokenCertificateValidator> certificateValidators;

    public CertChainPublicKeyResolver(OidcTenantConfig oidcConfig) {
        this.oidcConfig = oidcConfig;
        if (oidcConfig.certificateChain().trustStorePassword().isEmpty()) {
            throw new ConfigurationException(
                    "Truststore with configured password which keeps thumbprints of the trusted certificates must be present");
        }
        this.thumbprints = TrustStoreUtils.getTrustedCertificateThumbprints(
                oidcConfig.certificateChain().trustStoreFile().get(),
                oidcConfig.certificateChain().trustStorePassword().get(),
                oidcConfig.certificateChain().trustStoreCertAlias(),
                oidcConfig.certificateChain().trustStoreFileType());
        this.expectedLeafCertificateName = oidcConfig.certificateChain().leafCertificateName();
        this.certificateValidators = TenantFeatureFinder.find(oidcConfig, TokenCertificateValidator.class);
    }

    @Override
    public Key resolveKey(JsonWebSignature jws, List<JsonWebStructure> nestingContext)
            throws UnresolvableKeyException {

        try {
            List<X509Certificate> chain = jws.getCertificateChainHeaderValue();
            if (chain == null) {
                LOG.debug("Token does not have an 'x5c' certificate chain header");
                return null;
            }
            if (chain.isEmpty()) {
                LOG.debug("Token 'x5c' certificate chain is empty");
                return null;
            }

            // General certificate chain validation
            //TODO: support revocation lists
            CertificateHelper.checkValidity(chain, null);
            if (chain.size() == 1) {
                // CertificateHelper.checkValidity does not currently
                // verify the certificate signature if it is a single certificate chain
                final X509Certificate root = chain.get(0);
                root.verify(root.getPublicKey());
            }

            // Always do the root certificate thumbprint check
            LOG.debug("Checking a thumbprint of the root chain certificate");
            String rootThumbprint = TrustStoreUtils.calculateThumprint(chain.get(chain.size() - 1));
            if (!thumbprints.contains(rootThumbprint)) {
                LOG.error("Thumprint of the root chain certificate is invalid");
                throw new UnresolvableKeyException("Thumprint of the root chain certificate is invalid");
            }

            // Run custom validators if any
            if (!certificateValidators.isEmpty()) {
                LOG.debug("Running custom TokenCertificateValidators");
                for (TokenCertificateValidator validator : certificateValidators) {
                    validator.validate(oidcConfig, chain, jws.getUnverifiedPayload());
                }
            }

            // Finally, check the leaf certificate if required
            if (expectedLeafCertificateName.isPresent()) {
                // Compare the leaf certificate common name against the configured value
                String leafCertificateName = HttpSecurityUtils.getCommonName(chain.get(0).getSubjectX500Principal());
                if (!expectedLeafCertificateName.get().equals(leafCertificateName)) {
                    LOG.errorf("Wrong leaf certificate common name: %s", leafCertificateName);
                    throw new UnresolvableKeyException("Wrong leaf certificate common name");
                }
            } else if (certificateValidators.isEmpty()) {
                // No custom validators are registered and no leaf certificate CN is configured
                // Check that the truststore contains a leaf certificate thumbprint
                LOG.debug("Checking a thumbprint of the leaf chain certificate");
                String thumbprint = TrustStoreUtils.calculateThumprint(chain.get(0));
                if (!thumbprints.contains(thumbprint)) {
                    LOG.error("Thumprint of the leaf chain certificate is invalid");
                    throw new UnresolvableKeyException("Thumprint of the leaf chain certificate is invalid");
                }
            }

            return chain.get(0).getPublicKey();
        } catch (UnresolvableKeyException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnresolvableKeyException("Invalid certificate chain", ex);
        }
    }
}
