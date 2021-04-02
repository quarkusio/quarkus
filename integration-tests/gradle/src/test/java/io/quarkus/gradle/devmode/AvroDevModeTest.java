package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

public class AvroDevModeTest extends QuarkusDevGradleTestBase {
    @Override
    protected String projectDirectoryName() {
        return "avro-simple-project";
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/hello")).isEqualTo("MAIL,SMS");

        replace("src/main/avro/hello.avpr",
                ImmutableMap.of(" \"symbols\" : [\"MAIL\", \"SMS\"]", " \"symbols\" : [\"EMAIL\"]"));

        assertUpdatedResponseContains("/hello", "EMAIL");
    }
}
