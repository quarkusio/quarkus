package io.quarkus.it.spring.data.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order extends AbstractEntity {

    @OneToOne(fetch = FetchType.LAZY)
    private Cart cart;

    public Order() {
    }

    public Order(Cart cart) {
        this.cart = cart;
    }

    public Order(Long id, Cart cart) {
        this.id = id;
        this.cart = cart;
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }
}
