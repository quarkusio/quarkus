package io.quarkus.it.bouncycastle;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.NativeImageTest;

@NativeImageTest
public class BouncyCastleFipsJsseITCase extends BouncyCastleFipsJsseTestCase {

    @Test
    @Override
    public void testListProviders() throws Exception {
        doTestListProviders();
        checkLog(true);
    }
}
