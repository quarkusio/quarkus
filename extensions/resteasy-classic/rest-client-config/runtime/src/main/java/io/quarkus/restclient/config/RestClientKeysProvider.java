package io.quarkus.restclient.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class RestClientKeysProvider implements Supplier<Iterable<String>> {
    public static List<String> KEYS = new ArrayList<>();

    @Override
    public Iterable<String> get() {
        return KEYS;
    }
}
