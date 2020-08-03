package org.my.group;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.example.MyTestFactory;

@QuarkusTest
public class MyResourceTest {

    @Test
    public void testTestFixtures() {
        MyService myService = new MyTestFactory().createMyService();

        String result = myService.getSomething();

        Assertions.assertEquals("test-fixtures", result);
    }
}