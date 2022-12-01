package io.quarkus.it.envers;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.envers.Audited;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "nameConstraint", columnNames = "name"))
@Audited
public class Project {

    private Long id;
    private String name;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "projectSeq")
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
