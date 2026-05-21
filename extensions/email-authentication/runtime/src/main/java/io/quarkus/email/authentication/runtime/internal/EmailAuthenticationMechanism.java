package io.quarkus.email.authentication.runtime.internal;

import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;
import static io.quarkus.email.authentication.runtime.internal.CookieEmailAuthenticationCodeStorage.addPersistentLoginManager;
import static io.quarkus.email.authentication.runtime.internal.CookieEmailAuthenticationCodeStorage.removePersistentLoginManager;
import static io.quarkus.email.authentication.runtime.internal.EmailAuthenticationEventImpl.createAuthenticationCodeEvent;
import static io.quarkus.email.authentication.runtime.internal.EmailAuthenticationEventImpl.createEmptyEvent;
import static io.quarkus.email.authentication.runtime.internal.EmailAuthenticationEventImpl.createLoginEvent;
import static io.quarkus.email.authentication.runtime.internal.EmailAuthenticationRecorder.LIVE_RELOAD_ENCRYPTION_KEY;
import static io.quarkus.security.spi.runtime.SecurityEventHelper.fire;
import static io.quarkus.security.spi.runtime.SecurityEventHelper.isEventObserved;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.setRoutingContextAttribute;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.BeanManager;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.email.authentication.EmailAuthenticationCodeSender;
import io.quarkus.email.authentication.EmailAuthenticationCodeStorage;
import io.quarkus.email.authentication.EmailAuthenticationEvent;
import io.quarkus.email.authentication.EmailAuthenticationRequest;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.runtime.SecurityConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.quarkus.vertx.http.runtime.security.PersistentLoginManager;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.mail.mailencoder.EmailAddress;
import io.vertx.ext.web.RoutingContext;

final class EmailAuthenticationMechanism implements HttpAuthenticationMechanism {

    /**
     * Does not contain 0, O, 1, I, l to avoid confusion between number and lookalike letters.
     * Vowels (except Y) are removed as well to limit chance we get undesirable words (like rude words).
     */
    private static final String SAFE_CODE_CHARS = "23456789BCDFGHJKMNPQRSTVWXYZ";
    private static final String EMAIL = "email";
    private static final Logger LOG = Logger.getLogger(EmailAuthenticationMechanism.class);

    private final String loginPage;
    private final String errorPage;
    private final String postLocation;
    private final String locationCookie;
    private final String landingPage;
    private final CookieSameSite cookieSameSite;
    private final String cookiePath;
    private final String cookieDomain;
    private final PersistentLoginManager loginManager;
    private final Event<EmailAuthenticationEvent> emailAuthEvent;
    private final int priority;
    private final EmailAuthenticationCodeSender codeSender;
    private final EmailAuthenticationCodeStorage codeStorage;
    private final SecureRandom secureRandom;
    private final String postCodeGenerationLocation;
    private final String emailParameter;
    private final String codeParameter;
    private final int codeLength;
    private final String codeGenerationLocation;

    EmailAuthenticationMechanism(EmailAuthenticationConfig emailAuthenticationConfig, VertxHttpConfig vertxHttpConfig,
            EmailAuthenticationCodeSender codeSender, EmailAuthenticationCodeStorage codeStorage,
            @ConfigProperty(name = LIVE_RELOAD_ENCRYPTION_KEY) Optional<String> liveReloadEncryptionKey,
            BeanManager beanManager, Event<EmailAuthenticationEvent> emailAuthEvent, SecurityConfig securityConfig) {
        this.secureRandom = new SecureRandom();
        this.loginManager = new PersistentLoginManager(getEncryptionKey(vertxHttpConfig, liveReloadEncryptionKey),
                emailAuthenticationConfig.sessionCookie(), emailAuthenticationConfig.timeout().toMillis(),
                emailAuthenticationConfig.newSessionCookieInterval().toMillis(), emailAuthenticationConfig.httpOnlyCookie(),
                emailAuthenticationConfig.cookieSameSite().name(), emailAuthenticationConfig.cookiePath().orElse(null),
                emailAuthenticationConfig.sessionCookieMaxAge().map(Duration::toSeconds).orElse(-1L),
                emailAuthenticationConfig.cookieDomain().orElse(null));
        this.loginPage = startWithSlash(emailAuthenticationConfig.loginPage().orElse(null));
        this.errorPage = startWithSlash(emailAuthenticationConfig.errorPage().orElse(null));
        this.landingPage = startWithSlash(emailAuthenticationConfig.landingPage().orElse(null));
        this.postLocation = startWithSlash(emailAuthenticationConfig.postLocation());
        this.locationCookie = emailAuthenticationConfig.locationCookie();
        this.cookiePath = emailAuthenticationConfig.cookiePath().orElse(null);
        this.cookieDomain = emailAuthenticationConfig.cookieDomain().orElse(null);
        this.cookieSameSite = CookieSameSite.valueOf(emailAuthenticationConfig.cookieSameSite().name());
        this.emailAuthEvent = isEventObserved(createEmptyEvent(), beanManager, securityConfig.events().enabled())
                ? emailAuthEvent
                : null;
        this.priority = emailAuthenticationConfig.priority();
        this.codeSender = codeSender;
        this.codeStorage = codeStorage;
        this.postCodeGenerationLocation = startWithSlash(emailAuthenticationConfig.codePage().orElse(null));
        this.emailParameter = emailAuthenticationConfig.emailParameter();
        this.codeParameter = emailAuthenticationConfig.codeParameter();
        this.codeLength = emailAuthenticationConfig.codeLength();
        this.codeGenerationLocation = startWithSlash(emailAuthenticationConfig.codeGenerationLocation());
    }

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        if (isPostLocation(context)) {
            //we always re-auth if it is a post to the auth URL
            context.put(HttpAuthenticationMechanism.class.getName(), this);
            return runFormAuth(context, identityProviderManager);
        } else if (isCodeGenerationPath(context)) {
            return generateAndSendCode(context).invoke(() -> {
                if (context.response().ended() || context.failed()) {
                    // just to stay safe; we recommend to return Uni failure if the request to send the code was rejected
                    return;
                }
                if (postCodeGenerationLocation == null) {
                    context.response().setStatusCode(200);
                    context.response().end();
                } else {
                    String location = assembleRedirectLocation(context, postCodeGenerationLocation);
                    context.response().setStatusCode(302);
                    context.response().headers().add(LOCATION, location);
                    context.response().end();
                }
            }).onFailure(f -> !(f instanceof AuthenticationException)).transform(AuthenticationFailedException::new);
        } else {
            PersistentLoginManager.RestoreResult result = loginManager.restore(context);
            if (result != null) {
                String emailAddress = result.getPrincipal();
                context.put(HttpAuthenticationMechanism.class.getName(), this);
                return findSecurityIdentityByEmailAddress(identityProviderManager, context, emailAddress)
                        .invoke(securityIdentity -> {
                            if (LOG.isTraceEnabled()) {
                                LOG.tracef("Authenticated user '%s' with email address '%s' using the session cookie",
                                        securityIdentity.getPrincipal().getName(), emailAddress);
                            }
                            loginManager.save(securityIdentity, context, result, context.request().isSSL());
                        })
                        .onFailure().invoke(f -> {
                            LOG.debugf(f, "Could not find valid SecurityIdentity for email address '%s'", emailAddress);
                            loginManager.clear(context);
                        });
            }
            return Uni.createFrom().nullItem();
        }
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        if (isPostLocation(context)) {
            if (errorPage != null) {
                LOG.debugf("Serving email authentication error page %s for %s", errorPage, context);
                // This method would no longer be called if authentication had already occurred.
                return getRedirect(context, errorPage);
            }
        } else if (isCodeGenerationPath(context)) {
            if (errorPage != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debugf("Serving email authentication error page %s for %s", errorPage, context.normalizedPath());
                }
                return getRedirect(context, errorPage);
            }
        } else if (loginPage != null) {
            LOG.debugf("Serving email authentication login page %s for %s", loginPage, context);
            // we need to store the URL
            storeInitialLocation(context);
            return getRedirect(context, loginPage);
        }

        // redirect is disabled
        return Uni.createFrom().item(new ChallengeData(HttpResponseStatus.UNAUTHORIZED.code()));
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Set.of(EmailAuthenticationRequest.class);
    }

    @Override
    public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
        return Uni.createFrom().item(new HttpCredentialTransport(HttpCredentialTransport.Type.POST, postLocation, EMAIL));
    }

    @Override
    public int getPriority() {
        return priority;
    }

    private boolean isPostLocation(RoutingContext context) {
        return context.normalizedPath().endsWith(postLocation) && context.request().method().equals(HttpMethod.POST);
    }

    private Uni<SecurityIdentity> runFormAuth(RoutingContext exchange, IdentityProviderManager securityContext) {
        exchange.request().setExpectMultipart(true);
        return Uni.createFrom().emitter(uniEmitter -> {
            exchange.request().endHandler(ignored -> {
                try {
                    final String code = exchange.request().formAttributes().get(codeParameter);
                    if (code == null || code.isEmpty()) {
                        LOG.debugf(
                                "Could not authenticate as email authentication code was not present in the request posted to '%s'",
                                postLocation);
                        uniEmitter.complete(null);
                        return;
                    }
                    authenticateUsingCode(code, securityContext, exchange)
                            .subscribe().with(identity -> {
                                if (emailAuthEvent != null) {
                                    fireEvent(createLoginEvent(identity, code, exchange));
                                }

                                try {
                                    loginManager.save(identity, exchange, null, exchange.request().isSSL());
                                    if (landingPage != null || exchange.request().getCookie(locationCookie) != null) {
                                        handleRedirectBack(exchange);
                                        //we  have authenticated, but we want to just redirect back to the original page
                                        //so we don't actually authenticate the current request
                                        //instead we have just set a cookie so the redirected request will be authenticated
                                    } else {
                                        exchange.response().setStatusCode(200);
                                        exchange.response().end();
                                    }
                                    uniEmitter.complete(null);
                                } catch (Throwable t) {
                                    LOG.error("Unable to complete email authentication", t);
                                    uniEmitter.fail(t);
                                }
                            }, uniEmitter::fail);
                } catch (Throwable t) {
                    uniEmitter.fail(t);
                }
            });
            exchange.request().resume();
        });
    }

    private void handleRedirectBack(final RoutingContext exchange) {
        Cookie redirect = exchange.request().getCookie(locationCookie);
        String location;
        if (redirect != null) {
            verifyRedirectBackLocation(exchange.request().absoluteURI(), redirect.getValue());
            redirect.setSecure(exchange.request().isSSL());
            redirect.setSameSite(cookieSameSite);
            location = redirect.getValue();
            exchange.response().addCookie(redirect.setMaxAge(0));
        } else {
            location = assembleRedirectLocation(exchange, landingPage);
        }
        exchange.response().setStatusCode(302);
        exchange.response().headers().add(LOCATION, location);
        exchange.response().end();
    }

    private void verifyRedirectBackLocation(String requestURIString, String redirectUriString) {
        URI requestUri = URI.create(requestURIString);
        URI redirectUri = URI.create(redirectUriString);
        if (!requestUri.getAuthority().equals(redirectUri.getAuthority())
                || !requestUri.getScheme().equals(redirectUri.getScheme())) {
            LOG.errorf("Location cookie value %s does not match the current request URI %s's scheme, host or port",
                    redirectUriString, requestURIString);
            throw new AuthenticationCompletionException();
        }
    }

    private void storeInitialLocation(final RoutingContext exchange) {
        Cookie cookie = Cookie.cookie(locationCookie, exchange.request().absoluteURI())
                .setPath(cookiePath).setSameSite(cookieSameSite).setSecure(exchange.request().isSSL());
        if (cookieDomain != null) {
            cookie.setDomain(cookieDomain);
        }
        exchange.response().addCookie(cookie);
    }

    private Uni<SecurityIdentity> authenticateUsingCode(String code, IdentityProviderManager identityProviderManager,
            RoutingContext routingContext) {
        // allows the default code storage, which based on cookies, to reuse our login manager
        addPersistentLoginManager(routingContext, loginManager);

        return codeStorage.findEmailAddressByCode(code, routingContext)
                .flatMap(emailAddress -> {
                    removePersistentLoginManager(routingContext);
                    if (emailAddress == null || emailAddress.isEmpty()) {
                        return Uni.createFrom().failure(new AuthenticationFailedException(
                                "Cannot authenticate with unknown or invalid code: " + code));
                    }
                    LOG.debugf("Found email address '%s' for email authentication code '%s'", emailAddress, code);
                    return findSecurityIdentityByEmailAddress(identityProviderManager, routingContext, emailAddress)
                            .onFailure().invoke(f -> LOG.debugf(f, "Could not find SecurityIdentity for email address '%s' "
                                    + "(resolved for email authentication code '%s')", emailAddress, code));
                });
    }

    private boolean isCodeGenerationPath(RoutingContext context) {
        return context.normalizedPath().endsWith(codeGenerationLocation) && context.request().method().equals(HttpMethod.POST);
    }

    private Uni<SecurityIdentity> generateAndSendCode(RoutingContext context) {
        context.request().setExpectMultipart(true);
        return Uni.createFrom().emitter(uniEmitter -> {
            context.request().endHandler(ignored -> {
                try {
                    String email = context.request().formAttributes().get(emailParameter);
                    if (email == null || email.isEmpty()) {
                        LOG.debugf("Could not send code as form attribute '%s' was not present in the posted result for %s",
                                emailParameter, context);
                        uniEmitter.fail(new IllegalArgumentException("Form attribute '" + emailParameter
                                + "' is required for the POST path '" + codeGenerationLocation + "'"));
                    } else if (isInvalidEmailAddress(email)) {
                        LOG.debugf("Could not send code as email address '%s' is not valid for %s", email, context);
                        uniEmitter.fail(new IllegalArgumentException("Email address '" + email + "' is not valid"));
                    } else {
                        // code is generated lazily by design, since generating random is not cheap operation;
                        // the storage may decide to reject the incoming request based on username, IP, rate limit, ...
                        var codeRequest = new EmailAuthenticationCodeStorage.EmailAuthenticationCodeRequest() {

                            private volatile char[] generatedCode = null;

                            @Override
                            public char[] code() {
                                if (generatedCode == null) {
                                    generatedCode = generateCode();
                                }
                                return generatedCode;
                            }
                        };

                        addPersistentLoginManager(context, loginManager);
                        codeStorage.storeCode(codeRequest, email, context)
                                .chain(() -> {
                                    removePersistentLoginManager(context);

                                    if (codeRequest.generatedCode == null) {
                                        LOG.warnf("Email authentication code storage did not store code requested for "
                                                + "email address '%s'; storage must return failure for rejected requests",
                                                email);
                                        return Uni.createFrom().failure(new IllegalStateException(
                                                "Email authentication code storage did not store the code"));
                                    } else {
                                        LOG.debugf("Stored code for the code request with email address '%s'", email);
                                        return sendCode(codeRequest.generatedCode, email, context);
                                    }
                                })
                                .subscribe().with(
                                        ignored2 -> uniEmitter.complete(null),
                                        failure -> {
                                            removePersistentLoginManager(context);
                                            LOG.debugf(failure,
                                                    "Failed to store email authentication code requested for email address '%s'",
                                                    email);
                                            if (emailAuthEvent != null) {
                                                fireEvent(createAuthenticationCodeEvent(failure, email, context));
                                            }
                                            uniEmitter.fail(failure);
                                        });
                    }
                } catch (Throwable t) {
                    uniEmitter.fail(t);
                }
            });
            context.request().resume();
        });
    }

    private Uni<Void> sendCode(char[] code, String email, RoutingContext event) {
        return codeSender.sendCode(code, email)
                .invoke(() -> {
                    LOG.debugf("Sent code to to email address '%s'", email);
                    if (emailAuthEvent != null) {
                        fireEvent(createAuthenticationCodeEvent(email, code, event), () -> Arrays.fill(code, '0'));
                    } else {
                        Arrays.fill(code, '0');
                    }
                })
                .onFailure().invoke(failure -> {
                    if (LOG.isDebugEnabled()) {
                        LOG.debugf(failure, "Failed to send email authentication code '%s' generated for email address '%s'",
                                String.valueOf(code), email);
                    }
                    if (emailAuthEvent != null) {
                        fireEvent(createAuthenticationCodeEvent(failure, email, event));
                    }
                });
    }

    private char[] generateCode() {
        // this may seem like a lot, but since in the default implementation we don't have OTP brute-force protection,
        // and we are reusing the session enc key to encrypt cookie with the code request, and there is a chance that
        // some users won't rotate enc key, we need high entropy in order to limit possibility that someone will succeed
        // in brute-force attacks
        char[] code = new char[codeLength];
        for (int i = 0; i < codeLength; i++) {
            int randomIndex = secureRandom.nextInt(SAFE_CODE_CHARS.length());
            code[i] = SAFE_CODE_CHARS.charAt(randomIndex);
        }
        return code;
    }

    private void fireEvent(EmailAuthenticationEvent eventInstance) {
        fireEvent(eventInstance, null);
    }

    private void fireEvent(EmailAuthenticationEvent eventInstance, Runnable onFired) {
        if (onFired != null) {
            fire(emailAuthEvent, eventInstance).whenComplete((e, t) -> onFired.run());
        } else {
            fire(emailAuthEvent, eventInstance);
        }
    }

    private static String startWithSlash(String page) {
        if (page == null) {
            return null;
        }
        return page.startsWith("/") ? page : "/" + page;
    }

    private static String assembleRedirectLocation(RoutingContext exchange, String path) {
        return exchange.request().scheme() + "://" + exchange.request().authority() + path;
    }

    private static Uni<ChallengeData> getRedirect(final RoutingContext exchange, final String location) {
        String loc = assembleRedirectLocation(exchange, location);
        return Uni.createFrom().item(new ChallengeData(302, LOCATION, loc));
    }

    private static Uni<SecurityIdentity> findSecurityIdentityByEmailAddress(IdentityProviderManager identityProviderManager,
            RoutingContext routingContext, String emailAddress) {
        return identityProviderManager
                .authenticate(setRoutingContextAttribute(new EmailAuthenticationRequest(emailAddress), routingContext));
    }

    private static String getEncryptionKey(VertxHttpConfig vertxHttpConfig, Optional<String> liveReloadEncryptionKey) {
        return vertxHttpConfig.encryptionKey().orElseGet(() -> {
            var key = liveReloadEncryptionKey.orElseGet(EmailAuthenticationRecorder::generateEncryptionKey);
            LOG.warn("Encryption key was not specified for persistent Email authentication, using temporary key " + key);
            return key;
        });
    }

    private static boolean isInvalidEmailAddress(String maybeEmail) {
        try {
            new EmailAddress(maybeEmail); // just here to validate the email address
            return false;
        } catch (Exception e) {
            return true;
        }
    }
}
