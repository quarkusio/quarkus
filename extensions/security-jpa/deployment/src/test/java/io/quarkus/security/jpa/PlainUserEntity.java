package io.quarkus.security.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

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
