package org.jboss.shamrock.jwt.deployment;

import java.security.KeyException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Optional;

import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;

import io.smallrye.jwt.KeyUtils;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.shamrock.runtime.ConfigGroup;

@ConfigGroup
public class JWTAuthContextInfoGroup {
    private static final String NONE = "NONE";
    private static final Logger log = Logger.getLogger(JWTAuthContextInfoGroup.class);

    // The MP-JWT spec defined configuration properties

    /**
     * @since 1.1
     */
    @Inject
    @ConfigProperty(name = "mp.jwt.verify.publickey", defaultValue = NONE)
    public Optional<String> mpJwtublicKey;
    /**
     * @since 1.1
     */
    @Inject
    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = NONE)
    public String mpJwtIssuer;
    /**
     * @since 1.1
     */
    @Inject
    @ConfigProperty(name = "mp.jwt.verify.publickey.location", defaultValue = NONE)
    /**
     * @since 1.1
     */
    public Optional<String> mpJwtLocation;
    /**
     * Not part of the 1.1 release, but talked about.
     */
    @Inject
    @ConfigProperty(name = "mp.jwt.verify.requireiss", defaultValue = "true")
    public Optional<Boolean> mpJwtRequireIss;


    public JWTAuthContextInfo getContextInfo() {
        // Log the config values
        log.debugf("init, mpJwtublicKey=%s, mpJwtIssuer=%s, mpJwtLocation=%s",
                   mpJwtublicKey.orElse("missing"), mpJwtIssuer, mpJwtLocation.orElse("missing"));

        JWTAuthContextInfo contextInfo = new JWTAuthContextInfo();
        // Look to MP-JWT values first
        if (mpJwtublicKey.isPresent() && !NONE.equals(mpJwtublicKey.get())) {
            // Need to decode what this is...
            try {
                RSAPublicKey pk = (RSAPublicKey) KeyUtils.decodeJWKSPublicKey(mpJwtublicKey.get());
                contextInfo.setSignerKey(pk);
                log.debugf("mpJwtublicKey parsed as JWK(S)");
            } catch (Exception e) {
                // Try as PEM key value
                log.debugf("mpJwtublicKey failed as JWK(S), %s", e.getMessage());
                try {
                    RSAPublicKey pk = (RSAPublicKey) KeyUtils.decodePublicKey(mpJwtublicKey.get());
                    contextInfo.setSignerKey(pk);
                    log.debugf("mpJwtublicKey parsed as PEM");
                } catch (Exception e1) {
                    throw new DeploymentException(e1);
                }
            }
        }

        if (mpJwtIssuer != null && !mpJwtIssuer.equals(NONE)) {
            contextInfo.setIssuedBy(mpJwtIssuer);
        } else {
            // If there is no expected issuer configured, don't validate it; new in MP-JWT 1.1
            contextInfo.setRequireIssuer(false);
        }

        if (mpJwtRequireIss != null && mpJwtRequireIss.isPresent()) {
            contextInfo.setRequireIssuer(mpJwtRequireIss.get());
        } else {
            // Default is to require iss claim
            contextInfo.setRequireIssuer(true);
        }

        // The MP-JWT location can be a PEM, JWK or JWKS
        if (mpJwtLocation.isPresent() && !NONE.equals(mpJwtLocation.get())) {
            contextInfo.setJwksUri(mpJwtLocation.get());
            contextInfo.setFollowMpJwt11Rules(true);
            // If the key location was given rather than material, try to load the key material
            if(!mpJwtublicKey.isPresent()) {
                KeyLoader loader = new KeyLoader(mpJwtLocation.get());
                try {
                    PublicKey signingKey = loader.resolveKey("*");
                    contextInfo.setSignerKey((RSAPublicKey.class.cast(signingKey)));
                } catch (KeyException|ClassCastException e) {
                    log.warnf(e, "Failed to load key from: %s", mpJwtLocation.get());
                }
            }

        }

        return contextInfo;
    }

}
