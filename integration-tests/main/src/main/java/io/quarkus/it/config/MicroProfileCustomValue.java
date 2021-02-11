package io.quarkus.it.config;

/**
 * Some test POJO used to test custom MicroProfile Config converters.
 */
public class MicroProfileCustomValue {

    private final int number;

    public MicroProfileCustomValue(int number) {
        this.number = number;
    };

    public int getNumber() {
        return number;
    }
}
