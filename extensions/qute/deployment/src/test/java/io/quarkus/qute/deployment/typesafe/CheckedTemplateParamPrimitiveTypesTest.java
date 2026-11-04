package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.CheckedTemplateParam;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusExtensionTest;

public class CheckedTemplateParamPrimitiveTypesTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Templates.class)
                    .addAsResource(
                            new StringAsset("int: {intVal}, long: {longVal}, boolean: {boolVal}, double: {doubleVal}"),
                            "templates/CheckedTemplateParamPrimitiveTypesTest/primitives.txt")
                    .addAsResource(
                            new StringAsset("{firstName} {lastName} is {age} years old"),
                            "templates/CheckedTemplateParamPrimitiveTypesTest/fullName.txt"));

    @Test
    public void testAllPrimitiveTypes() {
        String result = Templates.primitives(42, 1000L, true, 3.14).render();
        assertTrue(result.contains("int: 42"));
        assertTrue(result.contains("long: 1000"));
        assertTrue(result.contains("boolean: true"));
        assertTrue(result.contains("double: 3.14"));
    }

    @Test
    public void testMultipleStringParameters() {
        String result = Templates.fullName("John", "Doe", 30).render();
        assertEquals("John Doe is 30 years old", result);
    }

    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance primitives(
                @CheckedTemplateParam("intVal") int i,
                @CheckedTemplateParam("longVal") long l,
                @CheckedTemplateParam("boolVal") boolean b,
                @CheckedTemplateParam("doubleVal") double d);

        static native TemplateInstance fullName(
                @CheckedTemplateParam("firstName") String first,
                @CheckedTemplateParam("lastName") String last,
                @CheckedTemplateParam("age") int userAge);
    }
}
