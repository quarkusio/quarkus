package io.quarkus.it.mockbean.hello;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HelloServiceTest {

    @Test
    void hello() {
        RecordB dataTwo = mock();
        when(dataTwo.dataHello()).thenReturn("foo");
        var service = new HelloServiceImpl(dataTwo);
        Assertions.assertEquals("foo", service.hello());
    }

}
