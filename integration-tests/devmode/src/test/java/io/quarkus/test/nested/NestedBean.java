package io.quarkus.test.nested;

import jakarta.inject.Singleton;

@Singleton
public class NestedBean {

    public String ping() {
        return "hello";
    }
}
