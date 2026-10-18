package io.quarkus.hibernate.orm.batch;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class OtherEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq2")
    public Long id;

    public OtherEntity() {
    }

    @Override
    public String toString() {
        return "OtherEntity#" + id;
    }
}
