package io.quarkus.arc.test.unused;

/**
 * Not a bean on its own but has a producer
 */
public class Delta {

    private String s;

    public Delta(String s) {
        this.s = s;
    }

    public String ping() {
        return s;
    }
}
