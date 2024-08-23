package io.quarkus.it.jpa.configurationless;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "typeConstraint", columnNames = "type"))

public class Cake {
    private Long id;
    private String type;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cakeSeq")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
