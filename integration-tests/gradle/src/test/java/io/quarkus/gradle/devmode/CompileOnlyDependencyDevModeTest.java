package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableMap;

public class CompileOnlyDependencyDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "compile-only-lombok";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "clean", "quarkusDev" };
    }

    protected void testDevMode() throws Exception {

        assertThat(getHttpResponse("/hello", 2, TimeUnit.MINUTES)).contains("hello lombok");
        assertThat(getHttpResponse("/hello/jpa", 2, TimeUnit.MINUTES)).isEqualTo("field");

        // make sure annotations go to the right place
        File testDir = getProjectDir();
        File entityMetamodelSourceFile = new File(testDir,
                "build/generated/sources/annotationProcessor/java/main/io/playground/MyEntity_.java");
        assertThat(entityMetamodelSourceFile).exists().content().contains("FIELD");
        File entityMetamodelClassFile = new File(testDir, "build/classes/java/main/io/playground/MyEntity_.class");
        assertThat(entityMetamodelClassFile).exists();

        replace("src/main/java/io/playground/MyData.java",
                ImmutableMap.of("private String other;", "private final String other;"));
        replace("src/main/java/io/playground/MyDataProducer.java",
                ImmutableMap.of("return new MyData(\"lombok\");", "return new MyData(\"lombok\", \"!\");"));

        replace("src/main/java/io/playground/HelloResource.java",
                ImmutableMap.of("return \"hello \" + myData.getMessage();",
                        "return \"hello \" + myData.getMessage() + myData.getOther();"));

        // Edit the entity to change the field name
        replace("src/main/java/io/playground/MyEntity.java", ImmutableMap.of("String field;", "String field2;"));

        // Edit the "Hello" message for the new field.
        replace("src/main/java/io/playground/HelloResource.java", ImmutableMap.of("return MyEntity_.FIELD;",
                "return MyEntity_.FIELD2;"));

        assertUpdatedResponseContains("/hello", "hello lombok!", 5, TimeUnit.MINUTES);
        assertUpdatedResponseContains("/hello/jpa", "field2", 5, TimeUnit.MINUTES);

        // make sure annotations go to the right place
        assertThat(entityMetamodelSourceFile).exists().content().contains("FIELD2");
        assertThat(entityMetamodelClassFile).exists();
    }
}
