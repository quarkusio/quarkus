package io.quarkus.vertx.http.testrunner;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UnitET {

    @Test
    public void unitStyleTest2() {
        Assertions.assertEquals("UNIT", UnitService.service());
    }

    @Test
    public void unitStyleTest() {
        HelloResource res = new HelloResource();
        Assertions.assertEquals("Hi", res.sayHello());
    }
}
