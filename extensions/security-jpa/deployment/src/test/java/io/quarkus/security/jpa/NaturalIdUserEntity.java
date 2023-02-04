package io.quarkus.security.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.NaturalId;

@UserDefinition
@Table(name = "test_user")
@Entity
public class NaturalIdUserEntity {
    @Id
    @GeneratedValue
    public Long id;

    @Column(name = "username")
    @Username
    @NaturalId
    public String name;

    @Column(name = "password")
    @Password(PasswordType.CLEAR)
    public String pass;

    @Roles
    public String role;
}
