package io.quarkus.it.mockbean;

public class DummyService1 implements DummyService {

    @Override
    public String returnDummyValue() {
        return "first";
    }
}
