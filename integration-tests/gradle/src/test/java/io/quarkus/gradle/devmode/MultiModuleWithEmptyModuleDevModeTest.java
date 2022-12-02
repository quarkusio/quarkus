package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

public class MultiModuleWithEmptyModuleDevModeTest extends QuarkusDevGradleTestBase {
    @Override
    protected String projectDirectoryName() {
        return "multi-module-with-empty-module";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "clean", ":modB:quarkusDev" };
    }

    @Override
    protected void testDevMode() throws Exception {
        //this has to download some additional test scoped deps (jacoco plugin)
        //which is why we go for a longer timeout here
        assertThat(getHttpResponse("/hello", 5, TimeUnit.MINUTES)).contains("foo bar");
    }
}
