package io.quarkus.email.authentication.runtime.internal;

import java.util.Arrays;

import org.jboss.logging.Logger;

import io.quarkus.arc.DefaultBean;
import io.quarkus.email.authentication.EmailAuthenticationCodeStorage.DefaultEmailAuthenticationCodeStorage;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.vertx.http.runtime.security.PersistentLoginManager;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@DefaultBean
final class CookieEmailAuthenticationCodeStorage implements DefaultEmailAuthenticationCodeStorage {

    private static final Logger LOG = Logger.getLogger(CookieEmailAuthenticationCodeStorage.class);
    private static final char EMAIL_TO_CODE_SEPARATOR = '-';
    private static final String PERSISTENT_LOGIN_MANAGER_KEY = "io.quarkus.email.authentication#login-manager";

    private final String cookieName;
    private final long maxAgeSeconds;
    private final long timeoutMillis;

    CookieEmailAuthenticationCodeStorage(EmailAuthenticationConfig config) {
        this.cookieName = config.codeCookie();
        this.maxAgeSeconds = config.codeExpiresIn().toSeconds();
        this.timeoutMillis = config.codeExpiresIn().toMillis();
    }

    @Override
    public Uni<Void> storeCode(EmailAuthenticationCodeRequest codeRequest, String emailAddress, RoutingContext event) {
        getLoginManager(event).save(createCookieValue(codeRequest.code(), emailAddress), event, cookieName, null,
                event.request().isSSL(), timeoutMillis, maxAgeSeconds);
        LOG.tracef("Stored email authentication code and email address '%s' in cookie '%s'", emailAddress, cookieName);
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<String> findEmailAddressByCode(String code, RoutingContext routingContext) {
        String codeRequest = getLoginManager(routingContext).getAndRemoveCookie(cookieName, routingContext);
        if (codeRequest == null) {
            LOG.tracef("Found no valid cookie '%s' for email authentication code '%s'", cookieName, code);
            return Uni.createFrom().nullItem();
        }

        // EmailAuthenticationMechanism#SAFE_CODE_CHARS does not contain our separator, hence use the last index
        var separatorIndex = codeRequest.lastIndexOf(EMAIL_TO_CODE_SEPARATOR);

        if (separatorIndex == -1) {
            return Uni.createFrom().failure(new IllegalStateException(
                    "Email authentication code request cookie '" + cookieName + "' has invalid format: " + codeRequest));
        }
        String emailAddress = codeRequest.substring(0, separatorIndex);
        String expectedCodeHash = codeRequest.substring(separatorIndex + 1);
        String receivedCodeHash = HashUtil.sha512(code);

        if (expectedCodeHash.equals(receivedCodeHash)) {
            LOG.tracef("Received correct email authentication code '%s' for email address '%s'", code, emailAddress);
            return Uni.createFrom().item(emailAddress);
        }

        LOG.tracef("Received wrong code '%s' for email address '%s', the received code hash '%s' does not match"
                + " the expected code hash '%s'", code, emailAddress, receivedCodeHash, expectedCodeHash);
        return Uni.createFrom().nullItem();
    }

    static void addPersistentLoginManager(RoutingContext routingContext, PersistentLoginManager loginManager) {
        routingContext.put(PERSISTENT_LOGIN_MANAGER_KEY, loginManager);
    }

    static void removePersistentLoginManager(RoutingContext routingContext) {
        routingContext.remove(PERSISTENT_LOGIN_MANAGER_KEY);
    }

    private static PersistentLoginManager getLoginManager(RoutingContext routingContext) {
        return routingContext.get(PERSISTENT_LOGIN_MANAGER_KEY);
    }

    private static String sha512(char[] code) {
        // this is safe for UTF-8 because we only expect chars from EmailAuthenticationMechanism#SAFE_CODE_CHARS
        byte[] bytes = new byte[code.length];
        try {
            for (int i = 0; i < code.length; i++) {
                bytes[i] = (byte) code[i];
            }
            return HashUtil.sha512(bytes);
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }

    private static String createCookieValue(char[] code, String emailAddress) {
        return emailAddress + EMAIL_TO_CODE_SEPARATOR + sha512(code);
    }
}
