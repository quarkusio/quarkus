package io.quarkus.security.jpa.reactive;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.security.jpa.RolesValue;

@Table(name = "test_role")
@Entity
public class PanacheRoleEntity extends PanacheEntity {

    @ManyToMany(mappedBy = "roles")
    public List<PanacheUserEntity> users;

    @Column(name = "role_name")
    @RolesValue
    public String role;

}
