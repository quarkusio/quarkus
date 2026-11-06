package io.quarkus.security.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@UserDefinition
@Table(name = "test_user")
@Entity
public class GenericGetterUserEntity extends AbstractGenericGetterUser<String> {
    @Id
    @GeneratedValue
    public Long id;
}
