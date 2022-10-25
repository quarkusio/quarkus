package io.quarkus.arc.test.interceptors.bridge;

import java.util.List;

import jakarta.inject.Singleton;

@Singleton
@Simple
public class ExampleResource extends AbstractResource<String> implements ExampleApi {

    // Just to try to confuse our bridge method impl. algorithm
    public String create(List<Object> dtos) {
        return dtos.toString();
    }

}
