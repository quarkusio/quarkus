package io.quarkus.websockets.next.test.security;

import jakarta.inject.Inject;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;

@WebSocket(path = "/product")
public class ProductEndpoint {

    private record Product(int id, String name) {
    }

    @Inject
    SecurityIdentity currentIdentity;

    @OnOpen
    String open() {
        return "ready";
    }

    @PermissionsAllowed("product:get")
    @OnTextMessage
    Product getProduct(int productId) {
        return new Product(productId, "Product " + productId);
    }

    @OnError
    String error(ForbiddenException t, WebSocketConnection conn) {
        return "forbidden:" + currentIdentity.getPrincipal().getName() + ",endpointId:" + conn.endpointId();
    }

    @PermissionChecker("product:get")
    boolean canGetProduct(int productId) {
        String username = currentIdentity.getPrincipal().getName();
        return currentIdentity.hasRole("admin") || canUserGetProduct(productId, username);
    }

    private static boolean canUserGetProduct(int productId, String username) {
        return productId == 2 && username.equals("user");
    }
}
