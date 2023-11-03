package io.quarkus.security.jpa.reactive;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.PasswordType;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.UserDefinition;
import io.quarkus.security.jpa.Username;

@UserDefinition
@Table(name = "test_user")
@Entity
public class MultipleRolesInCollectionUserEntity {
    @Id
    @GeneratedValue
    public Long id;

    @Column(name = "username")
    @Username
    public String name;

    @Column(name = "password")
    @Password(PasswordType.CLEAR)
    public String pass;

    @CollectionTable(name = "test_role", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role_name")
    @ElementCollection
    @Roles
    public List<String> roles = new ArrayList<>();

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
