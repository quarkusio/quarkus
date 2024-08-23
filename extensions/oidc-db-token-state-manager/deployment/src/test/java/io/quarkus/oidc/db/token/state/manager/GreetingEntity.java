package io.quarkus.oidc.db.token.state.manager;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Table(name = "greeting")
@Entity
public class GreetingEntity {

    @Id
    @GeneratedValue
    Long id;

    String greeting;

}
