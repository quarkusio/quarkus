/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.quarkus.vertx.http.runtime.security;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.credential.PasswordCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * The authentication handler responsible for BASIC authentication as described by RFC2617
 *
 */
@Singleton
public class BasicAuthenticationMechanism implements HttpAuthenticationMechanism {

    private static final Logger log = Logger.getLogger(BasicAuthenticationMechanism.class);

    public static final String SILENT = "silent";
    public static final String CHARSET = "charset";
    /**
     * A comma separated list of patterns and charsets. The pattern is a regular expression.
     *
     * Because different browsers user different encodings this allows for the correct encoding to be selected based
     * on the current browser. In general though it is recommended that BASIC auth not be used when passwords contain
     * characters outside ASCII, as some browsers use the current locate to determine encoding.
     *
     * This list must have an even number of elements, as it is interpreted as pattern,charset,pattern,charset,...
     */
    public static final String USER_AGENT_CHARSETS = "user-agent-charsets";

    private final String challenge;

    private static final String BASIC = "basic";
    private static final String BASIC_PREFIX = BASIC + " ";
    private static final String LOWERCASE_BASIC_PREFIX = BASIC_PREFIX.toLowerCase(Locale.ENGLISH);
    private static final int PREFIX_LENGTH = BASIC_PREFIX.length();
    private static final String COLON = ":";

    /**
     * If silent is true then this mechanism will only take effect if there is an Authorization header.
     *
     * This allows you to combine basic auth with form auth, so human users will use form based auth, but allows
     * programmatic clients to login using basic auth.
     */
    private final boolean silent;

    private final Charset charset;
    private final Map<Pattern, Charset> userAgentCharsets;

    public BasicAuthenticationMechanism(final String realmName) {
        this(realmName, false);
    }

    public BasicAuthenticationMechanism(final String realmName, final boolean silent) {
        this(realmName, silent, StandardCharsets.UTF_8, Collections.emptyMap());
    }

    public BasicAuthenticationMechanism(final String realmName, final boolean silent,
            Charset charset, Map<Pattern, Charset> userAgentCharsets) {
        this.challenge = BASIC_PREFIX + "realm=\"" + realmName + "\"";
        this.silent = silent;
        this.charset = charset;
        this.userAgentCharsets = Collections.unmodifiableMap(new LinkedHashMap<>(userAgentCharsets));
    }

    @Deprecated
    public BasicAuthenticationMechanism(final String realmName, final String mechanismName) {
        this(realmName, mechanismName, false);
    }

    @Deprecated
    public BasicAuthenticationMechanism(final String realmName, final String mechanismName, final boolean silent) {
        this(realmName, mechanismName, silent, StandardCharsets.UTF_8, Collections.emptyMap());
    }

    @Deprecated
    public BasicAuthenticationMechanism(final String realmName, final String mechanismName, final boolean silent,
            Charset charset, Map<Pattern, Charset> userAgentCharsets) {
        this.challenge = BASIC_PREFIX + "realm=\"" + realmName + "\"";
        this.silent = silent;
        this.charset = charset;
        this.userAgentCharsets = Collections.unmodifiableMap(new LinkedHashMap<>(userAgentCharsets));
    }

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager identityProviderManager) {
        List<String> authHeaders = context.request().headers().getAll(HttpHeaderNames.AUTHORIZATION);
        if (authHeaders != null) {
            for (String current : authHeaders) {
                if (current.toLowerCase(Locale.ENGLISH).startsWith(LOWERCASE_BASIC_PREFIX)) {

                    String base64Challenge = current.substring(PREFIX_LENGTH);
                    String plainChallenge = null;
                    byte[] decode = Base64.getDecoder().decode(base64Challenge);

                    Charset charset = this.charset;
                    if (!userAgentCharsets.isEmpty()) {
                        String ua = context.request().headers().get(HttpHeaderNames.USER_AGENT);
                        if (ua != null) {
                            for (Map.Entry<Pattern, Charset> entry : userAgentCharsets.entrySet()) {
                                if (entry.getKey().matcher(ua).find()) {
                                    charset = entry.getValue();
                                    break;
                                }
                            }
                        }
                    }

                    plainChallenge = new String(decode, charset);
                    int colonPos;
                    if ((colonPos = plainChallenge.indexOf(COLON)) > -1) {
                        String userName = plainChallenge.substring(0, colonPos);
                        char[] password = plainChallenge.substring(colonPos + 1).toCharArray();
                        log.debugf("Found basic auth header %s:***** (decoded using charset %s)", userName, charset);

                        UsernamePasswordAuthenticationRequest credential = new UsernamePasswordAuthenticationRequest(userName,
                                new PasswordCredential(password));
                        HttpSecurityUtils.setRoutingContextAttribute(credential, context);
                        return identityProviderManager.authenticate(credential);
                    }

                    // By this point we had a header we should have been able to verify but for some reason
                    // it was not correctly structured.
                    return Uni.createFrom().failure(new AuthenticationFailedException());
                }
            }
        }

        // No suitable header has been found in this request,
        return Uni.createFrom().optional(Optional.empty());
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        if (silent) {
            //if this is silent we only send a challenge if the request contained auth headers
            //otherwise we assume another method will send the challenge
            String authHeader = context.request().headers().get(HttpHeaderNames.AUTHORIZATION);
            if (authHeader == null) {
                return Uni.createFrom().optional(Optional.empty());
            }
        }
        ChallengeData result = new ChallengeData(
                HttpResponseStatus.UNAUTHORIZED.code(),
                HttpHeaderNames.WWW_AUTHENTICATE,
                challenge);
        return Uni.createFrom().item(result);
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Collections.singleton(UsernamePasswordAuthenticationRequest.class);
    }

    @Override
    public HttpCredentialTransport getCredentialTransport() {
        return new HttpCredentialTransport(HttpCredentialTransport.Type.AUTHORIZATION, BASIC);
    }

    @Override
    public int getPriority() {
        return 2000;
    }
}
