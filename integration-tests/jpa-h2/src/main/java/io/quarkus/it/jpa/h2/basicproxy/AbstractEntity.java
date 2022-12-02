package io.quarkus.it.jpa.h2.basicproxy;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "ENTITY")
@DiscriminatorColumn(name = "TYPE")
public abstract class AbstractEntity implements Serializable {

    public AbstractEntity() {
        // nop
    }

    @Id
    @Column(name = "TYPE", insertable = false, updatable = false)
    public String type;

    @Id
    @Column(name = "ID")
    public String id;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AbstractEntity that = (AbstractEntity) o;
        return Objects.equals(type, that.type) && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, id);
    }
}
