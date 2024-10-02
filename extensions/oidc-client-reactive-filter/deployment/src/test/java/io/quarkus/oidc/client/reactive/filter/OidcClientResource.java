package io.quarkus.oidc.client.reactive.filter;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/oidc-client")
public class OidcClientResource {

    @Inject
    @RestClient
    ProtectedResourceServiceAnnotationOidcClient protectedResourceServiceAnnotationOidcClient;

    @Inject
    @RestClient
    ProtectedResourceServiceConfigPropertyOidcClient protectedResourceServiceConfigPropertyOidcClient;

    @Inject
    @RestClient
    ProtectedResourceServiceCustomProviderConfigPropOidcClient protectedResourceServiceCustomProviderConfigPropOidcClient;

    @GET
    @Path("/annotation/user-name")
    public String annotationUserName() {
        return protectedResourceServiceAnnotationOidcClient.getUserName();
    }

    @GET
    @Path("/annotation/anonymous-user-name")
    public String annotationAnonymousUserName() {
        return protectedResourceServiceAnnotationOidcClient.getAnonymousUserName();
    }

    @GET
    @Path("/config-property/user-name")
    public String configPropertyUserName() {
        return protectedResourceServiceConfigPropertyOidcClient.getUserName();
    }

    @GET
    @Path("/config-property/anonymous-user-name")
    public String configPropertyAnonymousUserName() {
        return protectedResourceServiceConfigPropertyOidcClient.getAnonymousUserName();
    }

    @GET
    @Path("/custom-provider-config-property/user-name")
    public String customProviderConfigPropertyUserName() {
        return protectedResourceServiceCustomProviderConfigPropOidcClient.getUserName();
    }

    @GET
    @Path("/custom-provider-config-property/anonymous-user-name")
    public String customProviderConfigPropertyAnonymousUserName() {
        return protectedResourceServiceCustomProviderConfigPropOidcClient.getAnonymousUserName();
    }
}
