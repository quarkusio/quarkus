package io.quarkus.it.keycloak;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantFeature;
import io.quarkus.oidc.TokenCertificateValidator;
import io.quarkus.oidc.runtime.TrustStoreUtils;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
@Unremovable
@TenantFeature("bearer-chain-custom-validator")
public class BearerTenantTokenChainValidator implements TokenCertificateValidator {

    @Override
    public void validate(OidcTenantConfig oidcConfig, List<X509Certificate> chain, String tokenClaims)
            throws CertificateException {
        if (!"bearer-chain-custom-validator".equals(oidcConfig.tenantId.get())) {
            throw new RuntimeException("Unexpected tenant id");
        }
        String leafCertificateThumbprint = TrustStoreUtils.calculateThumprint(chain.get(0));
        JsonObject claims = new JsonObject(tokenClaims);
        if (!leafCertificateThumbprint.equals(claims.getString("leaf-certificate-thumbprint"))) {
            throw new CertificateException("Invalid leaf certificate");
        }
    }

}
