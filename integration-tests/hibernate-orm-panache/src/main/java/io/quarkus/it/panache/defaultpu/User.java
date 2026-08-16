package io.quarkus.it.panache.defaultpu;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.id.uuid.UuidGenerator;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue
    @GenericGenerator(type = UuidGenerator.class)
    public String id;

    @Column(name = "first_name")
    public String firstName;

    @Column(name = "last_name")
    public String lastName;

    public Integer age;
}
