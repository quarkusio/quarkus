package io.quarkus.security.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractGenericGetterUser<T> {

    @Column(name = "username")
    private T name;

    @Column(name = "password")
    private T pass;

    private T role;

    @Username
    public T getName() {
        return name;
    }

    @Password(PasswordType.CLEAR)
    public T getPass() {
        return pass;
    }

    @Roles
    public T getRole() {
        return role;
    }
}
