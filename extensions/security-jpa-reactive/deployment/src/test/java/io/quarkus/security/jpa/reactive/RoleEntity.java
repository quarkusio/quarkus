package io.quarkus.security.jpa.reactive;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import io.quarkus.security.jpa.RolesValue;

@Table(name = "test_role")
@Entity
public class RoleEntity {
    @Id
    @GeneratedValue
    public Long id;

    @ManyToMany(mappedBy = "roles")
    public List<ExternalRolesUserEntity> users;

    @Column(name = "role_name")
    @RolesValue
    public String role;

}
