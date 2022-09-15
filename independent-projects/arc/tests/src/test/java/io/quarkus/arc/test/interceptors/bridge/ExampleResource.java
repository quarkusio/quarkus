package io.quarkus.arc.test.interceptors.bridge;

import jakarta.inject.Singleton;
import java.util.List;

@Singleton
@Simple
public class ExampleResource extends AbstractResource<String> implements ExampleApi {

    // Just to try to confuse our bridge method impl. algorithm
    public String create(List<Object> dtos) {
        return dtos.toString();
    }

}
