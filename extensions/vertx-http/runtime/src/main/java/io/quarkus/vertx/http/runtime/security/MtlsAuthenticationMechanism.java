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

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.net.ssl.SSLPeerUnverifiedException;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.security.credential.CertificateCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.CertificateAuthenticationRequest;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.vertx.http.security.MTLS;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

/**
 * The authentication handler responsible for mTLS client authentication.
 */
public final class MtlsAuthenticationMechanism implements HttpAuthenticationMechanism {
    public static final int INCLUSIVE_AUTHENTICATION_PRIORITY = 3000;
    private static final String ROLES_MAPPER_ATTRIBUTE = "roles_mapper";
    private final boolean inclusiveAuthentication;
    private final ClientAuth tlsClientAuth;
    private final Optional<String> httpServerTlsConfigName;
    private final TlsConfiguration initialTlsConfiguration;
    private Function<X509Certificate, Set<String>> certificateToRoles = null;

    @Inject
    MtlsAuthenticationMechanism(@ConfigProperty(name = "quarkus.http.auth.inclusive") boolean inclusiveAuthentication) {
        this.inclusiveAuthentication = inclusiveAuthentication;
        this.tlsClientAuth = null;
        this.httpServerTlsConfigName = Optional.empty();
        this.initialTlsConfiguration = null;
    }

    public MtlsAuthenticationMechanism(MTLS.Builder.MTLSConfig mtlsConfig) {
        Objects.requireNonNull(mtlsConfig);
        this.certificateToRoles = mtlsConfig.certificateToRoles;
        this.tlsClientAuth = Objects.requireNonNull(mtlsConfig.tlsClientAuth);
        this.httpServerTlsConfigName = Objects.requireNonNull(mtlsConfig.httpServerTlsConfigName);
        this.initialTlsConfiguration = mtlsConfig.initialTlsConfiguration;
        this.inclusiveAuthentication = ConfigProvider.getConfig().getValue("quarkus.http.auth.inclusive", boolean.class);
    }

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager identityProviderManager) {
        HttpServerRequest request = context.request();

        if (!request.isSSL()) {
            return Uni.createFrom().nullItem();
        }

        Certificate certificate;

        try {
            certificate = request.sslSession().getPeerCertificates()[0];
        } catch (SSLPeerUnverifiedException e) {
            return Uni.createFrom().nullItem();
        }
        context.put(HttpAuthenticationMechanism.class.getName(), this);

        AuthenticationRequest authRequest = new CertificateAuthenticationRequest(
                new CertificateCredential((X509Certificate) certificate));
        authRequest.setAttribute(ROLES_MAPPER_ATTRIBUTE, certificateToRoles);
        return identityProviderManager
                .authenticate(HttpSecurityUtils.setRoutingContextAttribute(authRequest, context));
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        return Uni.createFrom().item(new ChallengeData(HttpResponseStatus.UNAUTHORIZED.code(),
                null, null));
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Collections.singleton(CertificateAuthenticationRequest.class);
    }

    @Override
    public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
        return Uni.createFrom().item(new HttpCredentialTransport(HttpCredentialTransport.Type.X509, "X509"));
    }

    @Override
    public int getPriority() {
        return inclusiveAuthentication ? INCLUSIVE_AUTHENTICATION_PRIORITY : DEFAULT_PRIORITY;
    }

    ClientAuth getTlsClientAuth() {
        return tlsClientAuth;
    }

    void setCertificateToRolesMapper(Function<X509Certificate, Set<String>> certificateToRoles) {
        this.certificateToRoles = certificateToRoles;
    }

    boolean isCertificateToRolesMapperSet() {
        return certificateToRoles != null;
    }

    Optional<String> getHttpServerTlsConfigName() {
        return httpServerTlsConfigName;
    }

    TlsConfiguration getInitialTlsConfiguration() {
        return initialTlsConfiguration;
    }
}
