package io.quarkus.funqy.test;

import java.util.Map;

import io.quarkus.funqy.Funq;

public class QueryFunction {

    @Funq
    public Simple simple(Simple simple) {
        return simple;
    }

    @Funq
    public Nested nested(Nested nested) {
        return nested;
    }

    @Funq
    public NestedCollection nestedCollection(NestedCollection nested) {
        return nested;
    }

    @Funq
    public Map<String, String> map(Map<String, String> nested) {
        return nested;
    }
}
