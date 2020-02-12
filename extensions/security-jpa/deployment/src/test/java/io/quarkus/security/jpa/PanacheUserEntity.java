package io.quarkus.security.jpa;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@UserDefinition
@Table(name = "test_user")
@Entity
public class PanacheUserEntity extends PanacheEntity {
    @Column(name = "username")
    @Username
    public String name;

    @Column(name = "password")
    @Password(PasswordType.CLEAR)
    public String pass;

    @JoinTable(name = "test_user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    @ManyToMany
    @Roles
    public List<PanacheRoleEntity> roles = new ArrayList<>();
}
