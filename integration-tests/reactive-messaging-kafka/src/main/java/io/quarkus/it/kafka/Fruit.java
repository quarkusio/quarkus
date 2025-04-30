package io.quarkus.it.kafka;

public class Fruit {

    public String name;
    public Fruits type;

    public Fruit(String name, Fruits type) {
        this.name = name;
        this.type = type;
    }

    public Fruit() {
        // Jackson will uses this constructor
    }

    public enum Fruits {
        BERRY,
        CITRUS,
        POME,
        STONE,
        TROPICAL;

        @Override
        public String toString() {
            return "fruit-" + name().toLowerCase();
        }

        Fruit create(String name) {
            return new Fruit(name, this);
        }
    }
}
