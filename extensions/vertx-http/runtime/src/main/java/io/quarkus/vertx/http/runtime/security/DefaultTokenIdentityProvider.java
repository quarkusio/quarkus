package io.quarkus.vertx.http.runtime.security;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Permission;
import java.security.Principal;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.arc.DefaultBean;
import io.quarkus.security.credential.Credential;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.vertx.http.runtime.AuthSessionConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.smallrye.mutiny.Uni;

/**
 * Login manager that can handle bearer token auth if JWT is not present
 */
@Singleton
@DefaultBean
public class DefaultTokenIdentityProvider implements IdentityProvider<TokenAuthenticationRequest> {

    private static final Logger log = Logger.getLogger(DefaultPersistentLoginManager.class);
    private static final String ENC_ALGORITHM = "AES/GCM/NoPadding";
    private static final int ENC_TAG_LENGTH = 128;
    public static final String QUARKUS_HTTP_AUTH_SESSION_NEW_TOKEN_NEEDED = "quarkus.http.auth.session.new-token-needed";

    private final SecretKey secretKey;
    private final long timeoutMillis;
    private final long newTokenIntervalMillis;

    @Inject
    public DefaultTokenIdentityProvider(HttpConfiguration httpConfiguration) {
        AuthSessionConfig config = httpConfiguration.sessionConfig;
        try {
            this.newTokenIntervalMillis = config.newTokenInterval.toMillis();
            this.timeoutMillis = config.timeout.toMillis();
            if (!config.encryptionKey.isPresent()) {
                this.secretKey = KeyGenerator.getInstance("AES").generateKey();
            } else if (config.encryptionKey.get().length() < 16) {
                throw new RuntimeException("Shared keys for persistent logins must be more than 16 characters long");
            } else {
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                sha256.update(config.encryptionKey.get().getBytes(StandardCharsets.UTF_8));
                this.secretKey = new SecretKeySpec(sha256.digest(), "AES");
            }
        } catch (Exception t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public Class<TokenAuthenticationRequest> getRequestType() {
        return TokenAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(TokenAuthenticationRequest request, AuthenticationRequestContext context) {
        String val = request.getToken().getToken();
        try {
            Cipher cipher = Cipher.getInstance(ENC_ALGORITHM);
            ByteBuffer byteBuffer = ByteBuffer.wrap(Base64.getDecoder().decode(val.getBytes(StandardCharsets.UTF_8)));
            int ivLength = byteBuffer.get();
            byte[] iv = new byte[ivLength];
            byteBuffer.get(iv);
            byte[] encrypted = new byte[byteBuffer.remaining()];
            byteBuffer.get(encrypted);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(ENC_TAG_LENGTH, iv));
            DataInputStream result = new DataInputStream(new ByteArrayInputStream(cipher.doFinal(encrypted)));
            long expireIdle = result.readLong();
            long now = System.currentTimeMillis();
            log.debugf("Current time: %s, Expire idle timeout: %s, expireIdle - now is: %d - %d = %d",
                    new Date(now).toString(), new Date(expireIdle).toString(), expireIdle, now, expireIdle - now);
            // We don't attempt renewal, idle timeout already expired.
            if (now > expireIdle) {
                // Enforce new login.
                return null;
            }
            boolean newTokenNeeded = (timeoutMillis - (expireIdle - now)) > newTokenIntervalMillis;
            log.debugf("Is new cookie needed? ( %d - ( %d - %d)) > %d : %b", timeoutMillis, expireIdle, now,
                    newTokenIntervalMillis, newTokenNeeded);

            String userName = result.readUTF();
            int noRoles = result.readInt();
            Set<String> roles = new HashSet<>();
            for (int i = 0; i < noRoles; ++i) {
                roles.add(result.readUTF());
            }
            return Uni.createFrom().item(new SecurityIdentity() {
                @Override
                public Principal getPrincipal() {
                    return new Principal() {
                        @Override
                        public String getName() {
                            return userName;
                        }
                    };
                }

                @Override
                public boolean isAnonymous() {
                    return false;
                }

                @Override
                public Set<String> getRoles() {
                    return Collections.unmodifiableSet(roles);
                }

                @Override
                public boolean hasRole(String role) {
                    return roles.contains(role);
                }

                @Override
                public <T extends Credential> T getCredential(Class<T> credentialType) {
                    if (credentialType == TokenCredential.class) {
                        return (T) request.getToken();
                    }
                    return null;
                }

                @Override
                public Set<Credential> getCredentials() {
                    return Collections.singleton(request.getToken());
                }

                @Override
                public <T> T getAttribute(String name) {
                    if (name.equals(QUARKUS_HTTP_AUTH_SESSION_NEW_TOKEN_NEEDED)) {
                        return (T) (Boolean) newTokenNeeded;
                    }
                    return null;
                }

                @Override
                public Map<String, Object> getAttributes() {
                    return Collections.singletonMap(QUARKUS_HTTP_AUTH_SESSION_NEW_TOKEN_NEEDED, newTokenNeeded);
                }

                @Override
                public Uni<Boolean> checkPermission(Permission permission) {
                    return Uni.createFrom().nothing();
                }
            });
        } catch (Exception e) {
            log.debug("Failed to restore persistent user session", e);
            return null;
        }

    }
}
