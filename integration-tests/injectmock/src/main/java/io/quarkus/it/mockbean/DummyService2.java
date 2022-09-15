package io.quarkus.it.mockbean;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

@Named("second")
@ApplicationScoped
public class DummyService2 implements DummyService {

    @Override
    public String returnDummyValue() {
        return "second";
    }
}
