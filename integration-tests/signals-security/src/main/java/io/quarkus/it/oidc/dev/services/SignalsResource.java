package io.quarkus.it.oidc.dev.services;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;

@Path("/signals")
public class SignalsResource {

    record MessageWrapper(String message, String requiredRole) {
    }

    private final Set<String> messages = ConcurrentHashMap.newKeySet();

    @Inject
    Signal<MessageWrapper> signal;

    @Inject
    SecurityIdentity securityIdentity;

    @GET
    @Path("roles-allowed/admin")
    public String sendSignalAndGetForAdminOnly() {
        String message = "roles-allowed-admin:" + securityIdentity.getPrincipal().getName() + " " + securityIdentity.getRoles();
        fireBlocking(message, "admin");
        return message;
    }

    @GET
    @Path("roles-allowed/user")
    public String sendSignalAndGetForUserOnly() {
        String message = "roles-allowed-user:" + securityIdentity.getPrincipal().getName() + " " + securityIdentity.getRoles();
        fireBlocking(message, "user");
        return message;
    }

    @DELETE
    @Path("clear")
    public void clearReceivedSignals() {
        clear();
    }

    @GET
    @Path("messages")
    public String getMessages() {
        return String.join("#", messages);
    }

    private void clear() {
        messages.clear();
    }

    void fire(String message, String requiredRole) {
        signal.publish(new MessageWrapper(message, requiredRole));
    }

    void fireBlocking(String message, String requiredRole) {
        signal.reactive().publish(new MessageWrapper(message, requiredRole)).await().indefinitely();
    }

    void observeMessage(@Receives MessageWrapper messageWrapper) {
        if (securityIdentity.hasRole(messageWrapper.requiredRole)) {
            messages.add(messageWrapper.message);
        }
    }

}
