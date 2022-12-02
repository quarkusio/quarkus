package io.quarkus.hibernate.orm.batch;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

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
