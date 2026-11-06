package io.quarkus.security.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractGenericUser<T> {

    @Column(name = "username")
    @Username
    public T name;

    @Column(name = "password")
    @Password(PasswordType.CLEAR)
    public T pass;

    @Roles
    public T role;
}
