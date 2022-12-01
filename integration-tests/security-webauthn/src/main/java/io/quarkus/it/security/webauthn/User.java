package io.quarkus.it.security.webauthn;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.smallrye.mutiny.Uni;

@Table(name = "user_table")
@Entity
public class User extends PanacheEntity {

    @Column(unique = true)
    public String userName;

    // non-owning side, so we can add more credentials later
    @OneToOne(mappedBy = "user")
    public WebAuthnCredential webAuthnCredential;

    public static Uni<User> findByUserName(String userName) {
        return find("userName", userName).firstResult();
    }
}
