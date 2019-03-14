package io.quarkus.jwt.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Permission;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.wildfly.security.auth.principal.NamePrincipal;
import org.wildfly.security.auth.realm.token.TokenSecurityRealm;
import org.wildfly.security.auth.server.RealmIdentity;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.authz.Attributes;
import org.wildfly.security.authz.AuthorizationIdentity;
import org.wildfly.security.authz.PermissionMappable;
import org.wildfly.security.authz.PermissionMapper;
import org.wildfly.security.authz.RoleDecoder;
import org.wildfly.security.authz.Roles;
import org.wildfly.security.authz.SimpleAttributesEntry;
import org.wildfly.security.evidence.BearerTokenEvidence;
import org.wildfly.security.permission.PermissionVerifier;

import io.quarkus.smallrye.jwt.runtime.auth.MpJwtValidator;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;

/**
 * Validate usage of the bearer token based realm
 */
public class TokenRealmUnitTest {

    @Test
    public void testTokenRealm() throws Exception {
        KeyPair keyPair = generateKeyPair();
        PublicKey pk1 = keyPair.getPublic();
        PrivateKey pk1Priv = keyPair.getPrivate();
        JWTAuthContextInfo contextInfo = new JWTAuthContextInfo();
        contextInfo.setSignerKey((RSAPublicKey) pk1);
        contextInfo.setIssuedBy("https://server.example.com");
        MpJwtValidator jwtValidator = new MpJwtValidator(contextInfo);

        TokenSecurityRealm tokenRealm = TokenSecurityRealm.builder()
                .claimToPrincipal(this::mpJwtLogic)
                .validator(jwtValidator)
                .build();

        String jwt = TokenUtils.generateTokenString("/Token1.json", pk1Priv, "testTokenRealm");
        BearerTokenEvidence tokenEvidence = new BearerTokenEvidence(jwt);
        tokenRealm.getRealmIdentity(tokenEvidence);
        RealmIdentity identity = tokenRealm.getRealmIdentity(tokenEvidence);
        assertNotNull(identity);
        assertTrue(identity.exists());
        AuthorizationIdentity authz = identity.getAuthorizationIdentity();
        // TODO add proper assertion
        //System.out.println(authz.getAttributes().keySet());
    }

    @Test
    public void testSecurityDomain() throws Exception {
        KeyPair keyPair = generateKeyPair();
        PublicKey pk1 = keyPair.getPublic();
        PrivateKey pk1Priv = keyPair.getPrivate();
        JWTAuthContextInfo contextInfo = new JWTAuthContextInfo();
        contextInfo.setSignerKey((RSAPublicKey) pk1);
        contextInfo.setIssuedBy("https://server.example.com");
        MpJwtValidator jwtValidator = new MpJwtValidator(contextInfo);
        TokenSecurityRealm tokenRealm = TokenSecurityRealm.builder()
                .claimToPrincipal(this::mpJwtLogic)
                .validator(jwtValidator)
                .build();

        SecurityDomain securityDomain = SecurityDomain.builder()
                .addRealm("MP-JWT", tokenRealm)
                .setRoleDecoder(new RoleDecoder() {
                    @Override
                    public Roles decodeRoles(AuthorizationIdentity authorizationIdentity) {
                        SimpleAttributesEntry groups = (SimpleAttributesEntry) authorizationIdentity.getAttributes()
                                .get("groups");
                        Set<String> roles = new HashSet<>(groups);
                        return new Roles() {
                            @Override
                            public boolean contains(String roleName) {
                                return roles.contains(roleName);
                            }

                            @Override
                            public Iterator<String> iterator() {
                                return roles.iterator();
                            }
                        };
                    }
                })
                .build()
                .setPermissionMapper(new PermissionMapper() {
                    @Override
                    public PermissionVerifier mapPermissions(PermissionMappable permissionMappable, Roles roles) {
                        return new PermissionVerifier() {
                            @Override
                            public boolean implies(Permission permission) {
                                return true;
                            }
                        };
                    }
                })
                .build();

        String jwt = TokenUtils.generateTokenString("/Token1.json", pk1Priv, "testTokenRealm");
        BearerTokenEvidence tokenEvidence = new BearerTokenEvidence(jwt);
        SecurityIdentity securityIdentity = securityDomain.authenticate(tokenEvidence);
        // TODO add proper assertion
        //System.out.println(securityIdentity.getAttributes().keySet());
    }

    private Principal mpJwtLogic(Attributes claims) {
        String pn = claims.getFirst("upn");
        if (pn == null) {
            pn = claims.getFirst("preferred_name");
        }
        if (pn == null) {
            pn = claims.getFirst("sub");
        }
        return new NamePrincipal(pn);
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048); // because that's the minimal accepted size
        return generator.generateKeyPair();
    }
}
