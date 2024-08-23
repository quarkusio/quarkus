package io.quarkus.container.image.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class ContainerImageConfigEffectiveGroupTest {

    public static final String USER_NAME_SYSTEM_PROPERTY = "user.name";
    private final Optional<String> EMPTY = Optional.empty();

    @Test
    void testFromLowercaseUsername() {
        testWithNewUsername("test", () -> {
            Optional<String> group = Optional.of(System.getProperty(USER_NAME_SYSTEM_PROPERTY));
            assertEquals(ContainerImageProcessor.getEffectiveGroup(EMPTY, false), Optional.of("test"));
        });
    }

    @Test
    void testFromUppercaseUsername() {
        testWithNewUsername("TEST", () -> {
            Optional<String> group = Optional.of(System.getProperty(USER_NAME_SYSTEM_PROPERTY));
            assertEquals(ContainerImageProcessor.getEffectiveGroup(EMPTY, false), Optional.of("test"));
        });
    }

    @Test
    void testFromSpaceInUsername() {
        testWithNewUsername("user name", () -> {
            Optional<String> group = Optional.of(System.getProperty(USER_NAME_SYSTEM_PROPERTY));
            assertEquals(ContainerImageProcessor.getEffectiveGroup(EMPTY, false), Optional.of("user-name"));
        });
    }

    @Test
    void testFromOther() {
        testWithNewUsername("WhateveR", () -> {
            Optional<String> group = Optional.of("OtheR");
            assertEquals(ContainerImageProcessor.getEffectiveGroup(group, false), Optional.of("other"));
        });
    }

    private void testWithNewUsername(String newUserName, Runnable test) {
        String previousUsernameValue = System.getProperty(USER_NAME_SYSTEM_PROPERTY);
        System.setProperty(USER_NAME_SYSTEM_PROPERTY, newUserName);

        test.run();

        System.setProperty(USER_NAME_SYSTEM_PROPERTY, previousUsernameValue);
    }
}
