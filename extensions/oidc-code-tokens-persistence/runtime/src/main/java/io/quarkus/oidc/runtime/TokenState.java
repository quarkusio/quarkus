package io.quarkus.oidc.runtime;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class TokenState extends PanacheEntityBase {
    @Id
    public String tokenState;
    public String idToken;
    public String accessToken;
    public String refreshToken;
}
