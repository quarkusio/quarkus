package io.quarkus.security.jpa.reactive;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.UserDefinition;
import io.quarkus.security.jpa.Username;

@UserDefinition
@Table(name = "test_user")
@Entity
public class BCryptUserEntity {
    @Id
    @GeneratedValue
    public Long id;

    @Column(name = "username")
    @Username
    public String name;

    @Column(name = "password")
    @Password
    public String pass;

    @Roles
    public String role;
}
