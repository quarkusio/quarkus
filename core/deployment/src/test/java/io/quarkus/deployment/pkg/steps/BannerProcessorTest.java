package io.quarkus.deployment.pkg.steps;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.file.Paths;

import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.steps.BannerProcessor;

public class BannerProcessorTest {

    class MyBannerProcessor extends BannerProcessor {
        public boolean test(String file) throws Exception {
            return this.isQuarkusCoreBanner(new URL("jar", null, 0, file));
        }
    }

    @Test
    public void checkQuarkusCoreBannerOnFilesystemWithSpecialCharacters() throws Exception {
        MyBannerProcessor processor = new MyBannerProcessor();

        assertFalse(processor.test(Paths.get("tmp", "Descărcări", "test", "something!").toFile().getAbsolutePath()));
        assertFalse(processor.test(Paths.get(Files.currentFolder().getAbsolutePath(), "src", "test", "resources", "test",
                "Descărcări", "without-class.jar!").toFile().getAbsolutePath()));
        assertTrue(processor.test(Paths.get(Files.currentFolder().getAbsolutePath(), "src", "test", "resources", "test",
                "Descărcări", "with-class.jar!").toFile().getAbsolutePath()));
    }
}
