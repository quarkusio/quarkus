package io.quarkus.hibernate.orm.batch;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Entity
public class MainEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq1")
    public Long id;

    @OneToMany
    public List<OtherEntity> others = new ArrayList<>();

    public MainEntity() {
    }

    @Override
    public String toString() {
        return "MainEntity#" + id;
    }
}
