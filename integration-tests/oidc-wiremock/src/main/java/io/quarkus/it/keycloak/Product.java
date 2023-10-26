package io.quarkus.it.keycloak;

public class Product {

    final String name;
    final int quantity;
    final String accessToken;

    Product(String name, int quantity, String accessToken) {
        this.name = name;
        this.quantity = quantity;
        this.accessToken = accessToken;
    }
}
