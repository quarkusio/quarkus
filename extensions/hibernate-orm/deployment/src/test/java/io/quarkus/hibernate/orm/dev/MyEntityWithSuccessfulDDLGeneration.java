package io.quarkus.hibernate.orm.dev;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.QueryHint;
import jakarta.persistence.Table;

@Entity(name = MyEntityWithSuccessfulDDLGeneration.NAME)
@Table(name = MyEntityWithSuccessfulDDLGeneration.TABLE_NAME)
@NamedQuery(name = "MyEntity.findAll", query = "SELECT e FROM MyEntity e ORDER BY e.name", hints = @QueryHint(name = "org.hibernate.cacheable", value = "true"))
@NamedNativeQuery(name = "MyEntity.nativeFindAll", query = "SELECT e FROM MyEntityTable e ORDER BY e.name")
public class MyEntityWithSuccessfulDDLGeneration {
    public static final String NAME = "MyEntity";
    public static final String TABLE_NAME = "MyEntityTable";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "myEntitySeq")
    public Long id;

    public String name;

    public MyEntityWithSuccessfulDDLGeneration() {
    }

    public MyEntityWithSuccessfulDDLGeneration(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "MyEntity:" + name;
    }
}
