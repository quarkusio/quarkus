package io.quarkus.hibernate.orm.formatmapper;

import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
public class MyJsonEntity {

    @Id
    public Long id;

    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, String> properties;

    public int amount1;
    public int amount2;

    @Formula("amount2 - amount1")
    public int amountDifference;

}
