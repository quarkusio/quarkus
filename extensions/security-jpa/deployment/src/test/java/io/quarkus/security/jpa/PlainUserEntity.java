package io.quarkus.security.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.NaturalId;

@Table(name = "test_user")
@Entity
public class PlainUserEntity {
    @Id
    @GeneratedValue
    public Long id;

    @Column(name = "username")
    @NaturalId
    public String name;

    @Column(name = "password")
    public String pass;

    public String role;
}
