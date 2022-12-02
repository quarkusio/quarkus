package io.quarkus.it.mockbean;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
public class GenericFieldsTest {

    @Inject
    MyContainerConsumer myContainerConsumer;

    @InjectMock
    MyContainer<String> stringContainer;

    @InjectMock
    MyContainer<Integer> integerContainer;

    @Test
    public void test() {
        Mockito.when(stringContainer.getValue()).thenReturn("hi");
        Mockito.when(integerContainer.getValue()).thenReturn(2);
        Assertions.assertEquals("hi hi", myContainerConsumer.createString());
    }
}
