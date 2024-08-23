package io.quarkus.hibernate.orm.batch;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

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
