package io.quarkus.it.jpa.jdbcmetadata;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.GenericGenerator;

@Entity
@GenericGenerator(name = "my-identity-generator", strategy = "sequence-identity", parameters = @org.hibernate.annotations.Parameter(name = "sequence", value = "MY_ID_SEQ"))
public class EntityWithSequenceIdentityId {
    @Id
    @GeneratedValue(generator = "my-identity-generator")
    private Long id;

    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
