package io.quarkustest.execannotations;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.Blocking;

public class ExecAnnotationInvalidTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(MyService.class)).assertException(e -> {
                assertInstanceOf(IllegalStateException.class, e);
                assertTrue(e.getMessage().contains("Wrong usage"));
                assertTrue(e.getMessage().contains("MyService.hello()"));
            });

    @Test
    public void test() {
        fail();
    }

    static class MyService {
        @Blocking
        String hello() {
            return "Hello world!";
        }
    }
}
