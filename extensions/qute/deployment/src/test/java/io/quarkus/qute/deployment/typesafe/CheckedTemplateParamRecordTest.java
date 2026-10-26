package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplateParam;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusExtensionTest;

public class CheckedTemplateParamRecordTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Greeting.class, UserInfo.class)
                    .addAsResource(new StringAsset("Hello {name}! You are {age} years old."),
                            "templates/CheckedTemplateParamRecordTest/Greeting.txt")
                    .addAsResource(new StringAsset("User: {userName}, Status: {userStatus}"),
                            "templates/CheckedTemplateParamRecordTest/UserInfo.txt"));

    @Test
    public void testCheckedTemplateParamWithRecord() {
        // Test with @CheckedTemplateParam where template variable name differs from record component name
        assertEquals("Hello World! You are 25 years old.",
                new Greeting("World", 25).render());
    }

    @Test
    public void testMultipleCheckedTemplateParamWithRecord() {
        // Test with multiple @CheckedTemplateParam annotations in a record
        assertEquals("User: Alice, Status: Active",
                new UserInfo("Alice", "Active").render());
    }

    // Template uses "name" and "age", record components have different names
    record Greeting(@CheckedTemplateParam("name") String userName,
            @CheckedTemplateParam("age") int userAge) implements TemplateInstance {
    }

    // Template uses "userName" and "userStatus"
    record UserInfo(@CheckedTemplateParam("userName") String name,
            @CheckedTemplateParam("userStatus") String status) implements TemplateInstance {
    }

}
