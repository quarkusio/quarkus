package io.quarkus.hibernate.orm.devconsole;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Type;

@Entity(name = MyEntityWithFailingDDLGeneration.NAME)
@Table(name = MyEntityWithFailingDDLGeneration.TABLE_NAME)
public class MyEntityWithFailingDDLGeneration {
    public static final String NAME = "MyEntity";
    public static final String TABLE_NAME = "MyEntityTable";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "myEntitySeq")
    public Long id;

    // The goal of this custom type is to trigger an error during DDL generation
    @Type(TypeWithUnsupportedSqlCode.class)
    public String name;

    public MyEntityWithFailingDDLGeneration() {
    }

    public MyEntityWithFailingDDLGeneration(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "MyEntity:" + name;
    }
}
