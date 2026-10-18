package org.acme.quarkus.sample;

import examples2.MutinyGreeter2Grpc;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class HelloWorld2Test {

    @Test
    public void someTest() {
        MutinyGreeter2Grpc.MutinyGreeter2Stub stub = null;
    }
}