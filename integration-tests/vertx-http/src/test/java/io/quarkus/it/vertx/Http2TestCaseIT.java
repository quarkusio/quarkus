package io.quarkus.it.vertx;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.NativeImageTest;

@NativeImageTest
public class Http2TestCaseIT extends Http2TestCase {
    @Override
    @Test
    public void testHttp2EnabledPlain() throws ExecutionException, InterruptedException {
        super.testHttp2EnabledPlain();
    }
}
