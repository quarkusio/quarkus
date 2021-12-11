package io.quarkus.oidc.runtime;

import javax.persistence.Entity;
import javax.persistence.Id;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
public class TokenState extends PanacheEntityBase {
    @Id
    public String tokenState;
    public String idToken;
    public String accessToken;
    public String refreshToken;
}
