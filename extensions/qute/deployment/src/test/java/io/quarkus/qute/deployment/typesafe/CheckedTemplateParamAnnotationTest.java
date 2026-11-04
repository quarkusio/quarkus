package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.CheckedTemplateParam;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusExtensionTest;

public class CheckedTemplateParamAnnotationTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Templates.class)
                    .addAsResource(new StringAsset("Hello {name}! You are {age} years old."),
                            "templates/CheckedTemplateParamAnnotationTest/greeting.txt")
                    .addAsResource(new StringAsset("User: {userName}, Status: {userStatus}"),
                            "templates/CheckedTemplateParamAnnotationTest/userInfo.txt"));

    @Test
    public void testTemplateParamAnnotation() {
        // Test with @CheckedTemplateParam where template variable name differs from Java parameter name
        assertEquals("Hello World! You are 25 years old.",
                Templates.greeting("World", 25).render());
    }

    @Test
    public void testMultipleTemplateParamAnnotations() {
        // Test with multiple @CheckedTemplateParam annotations
        assertEquals("User: Alice, Status: Active",
                Templates.userInfo("Alice", "Active").render());
    }

    @CheckedTemplate
    public static class Templates {

        // Template uses "name" and "age", Java method uses different parameter names
        static native TemplateInstance greeting(@CheckedTemplateParam("name") String userName,
                @CheckedTemplateParam("age") int userAge);

        // Template uses "userName" and "userStatus"
        static native TemplateInstance userInfo(@CheckedTemplateParam("userName") String name,
                @CheckedTemplateParam("userStatus") String status);

    }

}
