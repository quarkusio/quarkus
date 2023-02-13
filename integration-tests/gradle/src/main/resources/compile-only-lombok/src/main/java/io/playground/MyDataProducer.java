package io.playground;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Singleton
public class MyDataProducer {

    @Produces
    public MyData myData() {
        return new MyData("lombok");
    }
}
