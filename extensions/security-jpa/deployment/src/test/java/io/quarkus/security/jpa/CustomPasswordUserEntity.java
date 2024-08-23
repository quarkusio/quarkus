package io.quarkus.security.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@UserDefinition
@Table(name = "test_user")
@Entity
public class CustomPasswordUserEntity {
    @Id
    @GeneratedValue
    public Long id;

    @Column(name = "username")
    @Username
    public String name;

    @Column(name = "password")
    @Password(value = PasswordType.CUSTOM, provider = CustomPasswordProvider.class)
    public String pass;

    @Roles
    public String role;
}
