package io.playground;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

@Singleton
public class MyDataProducer {

    @Produces
    public MyData myData() {
        return new MyData("lombok");
    }
}
