package io.quarkus.it.main;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ParameterizedEnumTestCase {

    public enum MyEnum {
        VALUE1,
        VALUE2
    }

    @ParameterizedTest
    @EnumSource(value = MyEnum.class)
    public void testHelloEndpoint(MyEnum input) {

    }
}
