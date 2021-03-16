package io.quarkus.devtools.codestarts;

public interface DataKey {
    default String key() {
        return this.toString().toLowerCase().replace("_", "-");
    }
}
