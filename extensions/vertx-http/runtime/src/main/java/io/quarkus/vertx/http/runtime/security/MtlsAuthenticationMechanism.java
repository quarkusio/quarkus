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
import java.util.Set;

import javax.net.ssl.SSLPeerUnverifiedException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.security.credential.CertificateCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.CertificateAuthenticationRequest;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

/**
 * The authentication handler responsible for mTLS client authentication
 */
public class MtlsAuthenticationMechanism implements HttpAuthenticationMechanism {

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

        return identityProviderManager
                .authenticate(HttpSecurityUtils.setRoutingContextAttribute(new CertificateAuthenticationRequest(
                        new CertificateCredential(X509Certificate.class.cast(certificate))), context));
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
    public HttpCredentialTransport getCredentialTransport() {
        return new HttpCredentialTransport(HttpCredentialTransport.Type.X509, "X509");
    }
}
