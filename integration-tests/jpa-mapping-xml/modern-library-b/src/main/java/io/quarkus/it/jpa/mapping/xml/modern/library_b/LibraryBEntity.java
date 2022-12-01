package io.quarkus.it.jpa.mapping.xml.modern.library_b;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * An annotated entity whose mapping is overridden in orm.xml.
 */
@Entity
public class LibraryBEntity {

    @Id
    private long id;

    @Basic
    private String name;

    public LibraryBEntity() {
    }

    public LibraryBEntity(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}