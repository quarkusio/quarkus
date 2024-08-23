package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/frontend")
public class FrontendResource {
    @Inject
    @RestClient
    ProtectedResourceServiceOidcClient protectedResourceServiceOidcClient;

    @Inject
    @RestClient
    ProtectedResourceServiceNamedOidcClient protectedResourceServiceNamedOidcClient;

    @Inject
    @RestClient
    ProtectedResourceServiceOidcClient protectedResourceServiceRegisterProvider;

    @Inject
    @RestClient
    ProtectedResourceServiceNoOidcClient protectedResourceServiceNoOidcClient;

    @Inject
    @RestClient
    ProtectedResourceServiceNonDefaultOidcClient protectedResourceServiceNonDefaultOidcClient;

    @Inject
    @RestClient
    MisconfiguredClientFilter misconfiguredClientFilter;

    @Inject
    ManagedExecutor managedExecutor;

    private Object lock = new Object();
    private volatile String userName;

    @GET
    @Path("userOidcClient")
    public String userNameOidcClient() {
        return protectedResourceServiceOidcClient.getUserName();
    }

    @GET
    @Path("userNonDefaultOidcClient")
    public String userNameNonDefaultOidcClient() {
        return protectedResourceServiceNonDefaultOidcClient.getUserName();
    }

    @GET
    @Path("userNameMisconfiguredClientFilter")
    @Produces("text/plain")
    public String userNameMisconfiguredClientFilter() {
        try {
            return misconfiguredClientFilter.getUserName();
        } catch (Throwable t) {
            return t.getMessage();
        }
    }

    @GET
    @Path("userOidcClientManagedExecutor")
    public String userNameOidcClientManagedExecutor() throws Exception {
        managedExecutor.execute(new Runnable() {

            @Override
            public void run() {
                String userNameResponse = protectedResourceServiceNamedOidcClient.getUserName();
                synchronized (lock) {
                    userName = userNameResponse;
                    lock.notify();
                }
            }

        });

        synchronized (lock) {
            while (userName == null) {
                lock.wait();
            }
        }
        return userName;
    }

    @GET
    @Path("userRegisterProvider")
    public String userNameRegisterProvider() {
        return protectedResourceServiceRegisterProvider.getUserName();
    }

    @GET
    @Path("userNoOidcClient")
    public String userNameNoOidcClient() {
        return protectedResourceServiceNoOidcClient.getUserName();
    }
}
