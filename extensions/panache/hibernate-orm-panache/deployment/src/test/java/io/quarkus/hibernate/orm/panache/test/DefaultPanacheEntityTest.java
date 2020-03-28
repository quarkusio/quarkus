package io.quarkus.hibernate.orm.panache.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

public class DefaultPanacheEntityTest {

    @Test
    public void testDefaultToStringMethod() {
        MyPanacheEntity myPanacheEntityWithId = new MyPanacheEntity(2912L);
        assertEquals("MyPanacheEntity<2912>", myPanacheEntityWithId.toString());

        MyPanacheEntity myPanacheEntityWithNullId = new MyPanacheEntity(null);
        assertEquals("MyPanacheEntity<null>", myPanacheEntityWithNullId.toString());
    }

    @Test
    public void testDefaultEqualsMethod() {
        MyPanacheEntity myPanacheEntityOne = new MyPanacheEntity(2912L);
        MyPanacheEntity myPanacheEntityTwo = new MyPanacheEntity(2912L);
        assertEquals(myPanacheEntityOne, myPanacheEntityTwo);
    }

    @Test
    public void testDefaultHashCodeMethod() {
        MyPanacheEntity myPanacheEntityOne = new MyPanacheEntity(2912L);
        MyPanacheEntity myPanacheEntityTwo = new MyPanacheEntity(2912L);
        assertEquals(myPanacheEntityOne.hashCode(), myPanacheEntityTwo.hashCode());
    }

    static class MyPanacheEntity extends PanacheEntity {
        public MyPanacheEntity(Long id) {
            this.id = id;
        }
    }
}
