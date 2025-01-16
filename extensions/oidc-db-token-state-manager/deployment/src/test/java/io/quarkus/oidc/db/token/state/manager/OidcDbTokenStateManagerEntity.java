package io.quarkus.oidc.db.token.state.manager;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Table(name = "oidc_db_token_state_manager")
@Entity
public class OidcDbTokenStateManagerEntity {

    @Id
    String id;

    @Column(name = "id_token", length = 4000)
    String idToken;

    @Column(name = "refresh_token", length = 4000)
    String refreshToken;

    @Column(name = "access_token", length = 4000)
    String accessToken;

    @Column(name = "access_token_expires_in")
    Long accessTokenExpiresIn;

    @Column(name = "expires_in")
    Long expiresIn;
}
