package org.shamrock.jpa.tests.configurationless;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "typeConstraint", columnNames = "type"))

public class Cake {
    private Long id;
    private String type;

    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="cakeSeq")
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
