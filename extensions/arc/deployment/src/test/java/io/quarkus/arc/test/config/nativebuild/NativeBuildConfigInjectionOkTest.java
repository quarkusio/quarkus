package io.quarkus.arc.test.config.nativebuild;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class NativeBuildConfigInjectionOkTest {

    @RegisterExtension
    static final QuarkusProdModeTest TEST = new QuarkusProdModeTest()
            .setBuildNative(true)
            .withApplicationRoot(
                    root -> root.addClass(Ok.class));

    @Test
    void test() {
    }
}
