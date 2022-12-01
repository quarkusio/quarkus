package io.quarkus.devtools.codestarts;

import java.util.Locale;

public interface DataKey {
    default String key() {
        return this.toString().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
