package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Disabled;

import com.google.common.collect.ImmutableMap;

@Disabled
public class CompileOnlyDependencyDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "compile-only-lombok";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "clean", "quarkusDev", "-s" };
    }

    protected void testDevMode() throws Exception {

        assertThat(getHttpResponse("/hello", 2, TimeUnit.MINUTES)).contains("hello lombok");

        replace("src/main/java/io/playground/MyData.java",
                ImmutableMap.of("private String other;", "private final String other;"));
        replace("src/main/java/io/playground/MyDataProducer.java",
                ImmutableMap.of("return new MyData(\"lombok\");", "return new MyData(\"lombok\", \"!\");"));

        replace("src/main/java/io/playground/HelloResource.java",
                ImmutableMap.of("return \"hello \" + myData.getMessage();",
                        "return \"hello \" + myData.getMessage() + myData.getOther();"));

        assertUpdatedResponseContains("/hello", "hello lombok!", 5, TimeUnit.MINUTES);
    }
}
