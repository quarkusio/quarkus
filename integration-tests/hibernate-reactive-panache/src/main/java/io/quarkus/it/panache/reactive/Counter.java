package io.quarkus.it.panache.reactive;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
public class Counter extends PanacheEntityBase {

    @Id
    private Long id;
    private int count = 0;

    public Counter() {
    }

    public Counter(long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public int getCount() {
        return count;
    }

    @Override
    public String toString() {
        return String.valueOf(count);
    }

    public void increase() {
        this.count++;
    }
}
