package io.quarkus.security.jpa;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Table(name = "test_role")
@Entity
public class PanacheRoleEntity extends PanacheEntity {

    @ManyToMany(mappedBy = "roles")
    public List<PanacheUserEntity> users;

    @Column(name = "role_name")
    @RolesValue
    public String role;

}
