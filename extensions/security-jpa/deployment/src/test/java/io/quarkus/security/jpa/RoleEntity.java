package io.quarkus.security.jpa;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

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
