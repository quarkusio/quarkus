package io.quarkus.mongodb.panache;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

public class DefaultPanacheMongoEntityTest {

    @Test
    public void testDefaultEqualsMethod() {
        ObjectId id = ObjectId.get();
        MyPanacheMongoEntity myPanacheEntityOne = new MyPanacheMongoEntity(id);
        MyPanacheMongoEntity myPanacheEntityTwo = new MyPanacheMongoEntity(id);
        assertEquals(myPanacheEntityOne, myPanacheEntityTwo);
    }

    @Test
    public void testDefaultHashCodeMethod() {
        ObjectId id = ObjectId.get();
        MyPanacheMongoEntity myPanacheEntityOne = new MyPanacheMongoEntity(id);
        MyPanacheMongoEntity myPanacheEntityTwo = new MyPanacheMongoEntity(id);
        assertEquals(myPanacheEntityOne.hashCode(), myPanacheEntityTwo.hashCode());
    }

    static class MyPanacheMongoEntity extends PanacheMongoEntity {
        public MyPanacheMongoEntity(ObjectId id) {
            this.id = id;
        }
    }
}
