package io.quarkus.qute.generator;

public interface SomeInterface {

    default String png() {
        return "ping";
    }

    default boolean hasPng(String val) {
        return png().equals(val);
    }

}
