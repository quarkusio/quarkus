package io.quarkus.hibernate.reactive.panache.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

public class DefaultPanacheEntityToStringTest {

    @Test
    public void testDefaultToStringMethod() {
        MyPanacheEntity myPanacheEntityWithId = new MyPanacheEntity(2912l);
        assertEquals("MyPanacheEntity<2912>", myPanacheEntityWithId.toString());

        MyPanacheEntity myPanacheEntityWithNullId = new MyPanacheEntity(null);
        assertEquals("MyPanacheEntity<null>", myPanacheEntityWithNullId.toString());
    }

    static class MyPanacheEntity extends PanacheEntity {
        public MyPanacheEntity(Long id) {
            this.id = id;
        }
    }
}
