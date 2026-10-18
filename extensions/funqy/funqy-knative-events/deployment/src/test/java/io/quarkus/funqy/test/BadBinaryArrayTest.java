package io.quarkus.funqy.test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class BadBinaryArrayTest {
    @RegisterExtension
    static QuarkusExtensionTest test1 = assertException(BadBinaryOutputCE.class);
    @RegisterExtension
    static QuarkusExtensionTest test2 = assertException(BadBinaryOutputRaw.class);
    @RegisterExtension
    static QuarkusExtensionTest test3 = assertException(BadBinaryInputCE.class);
    @RegisterExtension
    static QuarkusExtensionTest test4 = assertException(BadBinaryInputRaw.class);

    private static QuarkusExtensionTest assertException(Class<?> clazz) {
        return new QuarkusExtensionTest()
                .withApplicationRoot(jar -> jar.addClass(clazz))
                .assertException(throwable -> {
                    // Traverse the cause chain to find the IllegalStateException
                    Throwable current = throwable;
                    IllegalStateException foundException = null;

                    while (current != null) {
                        if (current instanceof IllegalStateException) {
                            foundException = (IllegalStateException) current;
                            break;
                        }
                        current = current.getCause();
                    }

                    if (foundException == null) {
                        fail("Expected IllegalStateException but got: " + throwable);
                    }

                    String message = foundException.getMessage();
                    if (message == null) {
                        fail("Exception message is null");
                    }

                    // Verify the exception message mentions the issue with Byte[] vs byte[]
                    if (!message.contains("Byte[]") || !message.contains("byte[]")) {
                        fail("Exception message should mention both 'Byte[]' and 'byte[]'. Got: " + message);
                    }
                });
    }

    @Test
    void test() {
        assertTrue(true);
    }
}
