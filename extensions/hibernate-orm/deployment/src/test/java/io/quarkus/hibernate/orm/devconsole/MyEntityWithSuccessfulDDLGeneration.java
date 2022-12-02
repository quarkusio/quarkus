package io.quarkus.hibernate.orm.devconsole;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import javax.persistence.Table;

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
