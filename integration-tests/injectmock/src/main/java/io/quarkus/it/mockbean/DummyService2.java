package io.quarkus.it.mockbean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

@Named("second")
@ApplicationScoped
public class DummyService2 implements DummyService {

    @Override
    public String returnDummyValue() {
        return "second";
    }
}
