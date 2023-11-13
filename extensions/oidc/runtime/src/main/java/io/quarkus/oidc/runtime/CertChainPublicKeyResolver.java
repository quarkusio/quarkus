package io.quarkus.oidc.runtime;

import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.lang.UnresolvableKeyException;

import io.quarkus.oidc.OidcTenantConfig.CertificateChain;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.vertx.ext.auth.impl.CertificateHelper;

public class CertChainPublicKeyResolver implements RefreshableVerificationKeyResolver {
    private static final Logger LOG = Logger.getLogger(OidcProvider.class);
    final Set<String> thumbprints;

    public CertChainPublicKeyResolver(CertificateChain chain) {
        if (chain.trustStorePassword.isEmpty()) {
            throw new ConfigurationException(
                    "Truststore with configured password which keeps thumbprints of the trusted certificates must be present");
        }
        this.thumbprints = TrustStoreUtils.getTrustedCertificateThumbprints(chain.trustStoreFile.get(),
                chain.trustStorePassword.get(), chain.trustStoreCertAlias, chain.getTrustStoreFileType());
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
            String thumbprint = TrustStoreUtils.calculateThumprint(chain.get(0));
            if (!thumbprints.contains(thumbprint)) {
                throw new UnresolvableKeyException("Certificate chain thumprint is invalid");
            }
            //TODO: support revocation lists
            CertificateHelper.checkValidity(chain, null);
            if (chain.size() == 1) {
                // CertificateHelper.checkValidity does not currently
                // verify the certificate signature if it is a single certificate chain
                final X509Certificate root = chain.get(0);
                root.verify(root.getPublicKey());
            }
            return chain.get(0).getPublicKey();
        } catch (Exception ex) {
            throw new UnresolvableKeyException("Invalid certificate chain", ex);
        }
    }
}