package io.quarkus.it.panache;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity(name = "content")
@Inheritance(strategy = InheritanceType.JOINED)
public class Content extends EntityBase {
    @Column(nullable = false)
    public String name;

}
