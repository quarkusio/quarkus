package io.quarkus.it.banner;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.test.QuarkusProdModeTest;

public class BannerDisplayedOnceTest {

    private static final String BANNER_MARKER = "UNIQUE_BANNER_MARKER_FOR_TEST";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ExitMain.class)
                    .addAsResource(new StringAsset(BANNER_MARKER + "\n"), "custom-banner.txt")
                    .addAsResource(new StringAsset("quarkus.banner.path=custom-banner.txt\n"),
                            "application.properties"))
            .setApplicationName("banner-test")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setRun(true)
            .setExpectExit(true);

    @Test
    public void bannerShouldBeDisplayedExactlyOnce() {
        String output = config.getStartupConsoleOutput();
        assertThat(output).contains(BANNER_MARKER);

        int firstIndex = output.indexOf(BANNER_MARKER);
        int secondIndex = output.indexOf(BANNER_MARKER, firstIndex + BANNER_MARKER.length());
        assertThat(secondIndex)
                .as("Banner should appear exactly once in output but was found at positions %d and %d",
                        firstIndex, secondIndex)
                .isEqualTo(-1);
    }

    @QuarkusMain
    public static class ExitMain implements QuarkusApplication {
        @Override
        public int run(String... args) {
            return 0;
        }
    }
}
