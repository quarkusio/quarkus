package io.quarkus.smallrye.jwt.runtime.auth;

import static io.undertow.util.Headers.AUTHORIZATION;
import static io.undertow.util.Headers.WWW_AUTHENTICATE;
import static io.undertow.util.StatusCodes.UNAUTHORIZED;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.undertow.UndertowLogger;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.HttpServerExchange;

/**
 * An AuthenticationMechanism that validates a caller based on a MicroProfile JWT bearer token
 */
public class JWTAuthMechanism implements AuthenticationMechanism {
    @Inject
    private JWTAuthContextInfo authContextInfo;

    private IdentityManager identityManager;

    public JWTAuthMechanism(JWTAuthContextInfo authContextInfo, IdentityManager identityManager) {
        this.authContextInfo = authContextInfo;
        this.identityManager = identityManager;
    }

    public JWTAuthMechanism(IdentityManager identityManager) {
        this.identityManager = identityManager;
    }

    /**
     * Extract the Authorization header and validate the bearer token if it exists. If it does, and is validated, this
     * builds the org.jboss.security.SecurityContext authenticated Subject that drives the container APIs as well as
     * the authorization layers.
     *
     * @param exchange - the http request exchange object
     * @param securityContext - the current security context that
     * @return one of AUTHENTICATED, NOT_AUTHENTICATED or NOT_ATTEMPTED depending on the header and authentication outcome.
     */
    @Override
    public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext securityContext) {
        List<String> authHeaders = exchange.getRequestHeaders().get(AUTHORIZATION);
        if (authHeaders != null) {
            String bearerToken = null;
            for (String current : authHeaders) {
                if (current.toLowerCase(Locale.ENGLISH).startsWith("bearer ")) {
                    bearerToken = current.substring(7);
                    if (UndertowLogger.SECURITY_LOGGER.isTraceEnabled()) {
                        UndertowLogger.SECURITY_LOGGER.tracef("Bearer token: %s", bearerToken);
                    }
                    try {
                        //identityManager = securityContext.getIdentityManager();
                        JWTCredential credential = new JWTCredential(bearerToken, authContextInfo);
                        if (UndertowLogger.SECURITY_LOGGER.isTraceEnabled()) {
                            UndertowLogger.SECURITY_LOGGER.tracef("Bearer token: %s", bearerToken);
                        }
                        // Install the JWT principal as the caller
                        Account account = identityManager.verify(credential.getName(), credential);
                        if (account != null) {
                            JsonWebToken jwtPrincipal = (JsonWebToken) account.getPrincipal();
                            //MPJWTProducer.setJWTPrincipal(jwtPrincipal);
                            JWTAccount jwtAccount = new JWTAccount(jwtPrincipal, account);
                            securityContext.authenticationComplete(jwtAccount, "MP-JWT", false);
                            /*
                             * // Workaround authenticated JsonWebToken not being installed as user principal
                             * // https://issues.jboss.org/browse/WFLY-9212
                             * org.jboss.security.SecurityContext jbSC = SecurityContextAssociation.getSecurityContext();
                             * Subject subject = jbSC.getUtil().getSubject();
                             * jbSC.getUtil().createSubjectInfo(jwtPrincipal, bearerToken, subject);
                             * RoleGroup roles = extract(subject);
                             * jbSC.getUtil().setRoles(roles);
                             */
                            UndertowLogger.SECURITY_LOGGER.debugf("Authenticated caller(%s) for path(%s) with roles: %s",
                                    credential.getName(), exchange.getRequestPath(), account.getRoles());
                            return AuthenticationMechanismOutcome.AUTHENTICATED;
                        } else {
                            UndertowLogger.SECURITY_LOGGER.info("Failed to authenticate JWT bearer token");
                            return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
                        }
                    } catch (Exception e) {
                        UndertowLogger.SECURITY_LOGGER.infof(e, "Failed to validate JWT bearer token");
                        return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
                    }
                }
            }
        }

        // No suitable header has been found in this request,
        return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
    }

    @Override
    public ChallengeResult sendChallenge(HttpServerExchange exchange, SecurityContext securityContext) {
        exchange.getResponseHeaders().add(WWW_AUTHENTICATE, "Bearer {token}");
        UndertowLogger.SECURITY_LOGGER.debugf("Sending Bearer {token} challenge for %s", exchange);
        return new ChallengeResult(true, UNAUTHORIZED);
    }

    /**
     * Extract the Roles group and return it as a RoleGroup
     *
     * @param subject authenticated subject
     * @return RoleGroup from "Roles"
     *         protected RoleGroup extract(Subject subject) {
     *         Optional<Principal> match = subject.getPrincipals()
     *         .stream()
     *         .filter(g -> g.getName().equals(SecurityConstants.ROLES_IDENTIFIER))
     *         .findFirst();
     *         Group rolesGroup = (Group) match.get();
     *         RoleGroup roles = new SimpleRoleGroup(rolesGroup);
     *         return roles;
     *         }
     */
}
