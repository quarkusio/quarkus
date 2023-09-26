package io.quarkus.arc.test.config.nativebuild;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class NativeBuildConfigInjectionFailTest {

    @RegisterExtension
    static final QuarkusProdModeTest TEST = new QuarkusProdModeTest()
            .setBuildNative(true)
            .withApplicationRoot(
                    root -> root.addClass(Fail.class))
            // ImageGenerationFailureException is private
            .setExpectedException(RuntimeException.class);

    @Test
    void test() {
        fail();
    }
}
