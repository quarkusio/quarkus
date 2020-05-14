package io.quarkus.smallrye.graphql.deployment;

/**
 * Just a test pojo that contains a random number
 */
public class TestRandom {
    private double value;

    public TestRandom() {
        this(Math.random());
    }

    public TestRandom(double value) {
        super();
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "TestRandom{" + "value=" + value + '}';
    }

}
