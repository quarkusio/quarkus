package io.quarkus.security.jpa;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

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
