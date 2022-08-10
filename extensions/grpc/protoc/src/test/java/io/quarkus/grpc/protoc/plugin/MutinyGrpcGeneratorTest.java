package io.quarkus.grpc.protoc.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MutinyGrpcGeneratorTest {

    @Test
    public void testAdaptMethodName() {
        assertEquals("sayHello", MutinyGrpcGenerator.adaptMethodName("SayHello"));
        assertEquals("sayHello", MutinyGrpcGenerator.adaptMethodName("Say_Hello"));
        assertEquals("sayHello", MutinyGrpcGenerator.adaptMethodName("Say_hello"));
        assertEquals("_SayHello", MutinyGrpcGenerator.adaptMethodName("_Say_hello"));
        assertEquals("sayHelloAndBye", MutinyGrpcGenerator.adaptMethodName("Say_Hello_and_Bye"));
        assertEquals("return_", MutinyGrpcGenerator.adaptMethodName("return"));
    }

}
