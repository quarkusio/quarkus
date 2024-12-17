package io.quarkus.it.security.webauthn;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.smallrye.mutiny.Uni;

@Table(name = "user_table")
@Entity
public class User extends PanacheEntity {

    @Column(unique = true)
    public String username;

    // non-owning side, so we can add more credentials later
    @OneToOne(mappedBy = "user")
    public WebAuthnCredential webAuthnCredential;

    public static Uni<User> findByUsername(String username) {
        return find("username", username).firstResult();
    }
}
