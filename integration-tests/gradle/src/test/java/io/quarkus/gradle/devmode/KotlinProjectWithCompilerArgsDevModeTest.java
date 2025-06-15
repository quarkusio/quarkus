package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableMap;

public class KotlinProjectWithCompilerArgsDevModeTest extends QuarkusDevGradleTestBase {
    @Override
    protected String projectDirectoryName() {
        return "kotlin-grpc-project";
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/graphql/schema.graphql", 2, TimeUnit.MINUTES)).contains("[Banana!]!");

        replace("src/main/kotlin/org/acme/GraphQLResource.kt", ImmutableMap.of("yellow", "blue"));

        assertUpdatedResponseContains("/graphql/schema.graphql", "[Banana!]!");
    }
}
