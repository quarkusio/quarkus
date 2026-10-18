package io.quarkus.it.keycloak;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenCertificateValidator;
import io.quarkus.oidc.runtime.TrustStoreUtils;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
@Unremovable
public class BearerGlobalTokenChainValidator implements TokenCertificateValidator {

    @Override
    public void validate(OidcTenantConfig oidcConfig, List<X509Certificate> chain, String tokenClaims)
            throws CertificateException {
        String rootCertificateThumbprint = TrustStoreUtils.calculateThumprint(chain.get(chain.size() - 1));
        JsonObject claims = new JsonObject(tokenClaims);
        if (!rootCertificateThumbprint.equals(claims.getString("root-certificate-thumbprint"))) {
            throw new CertificateException("Invalid root certificate");
        }
    }

}
