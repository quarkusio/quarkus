package io.quarkus.container.image.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class ContainerImageConfigEffectiveGroupTest {

    public static final String USER_NAME_SYSTEM_PROPERTY = "user.name";

    private ContainerImageConfig sut = new ContainerImageConfig();

    @Test
    void testFromLowercaseUsername() {
        testWithNewUsername("test", () -> {
            sut.group = Optional.of(System.getProperty(USER_NAME_SYSTEM_PROPERTY));
            assertEquals(sut.getEffectiveGroup(), Optional.of("test"));
        });
    }

    @Test
    void testFromUppercaseUsername() {
        testWithNewUsername("TEST", () -> {
            sut.group = Optional.of(System.getProperty(USER_NAME_SYSTEM_PROPERTY));
            assertEquals(sut.getEffectiveGroup(), Optional.of("test"));
        });
    }

    @Test
    void testFromSpaceInUsername() {
        testWithNewUsername("user name", () -> {
            sut.group = Optional.of(System.getProperty(USER_NAME_SYSTEM_PROPERTY));
            assertEquals(sut.getEffectiveGroup(), Optional.of("user-name"));
        });
    }

    @Test
    void testFromOther() {
        testWithNewUsername("WhateveR", () -> {
            sut.group = Optional.of("OtheR");
            assertEquals(sut.getEffectiveGroup(), Optional.of("OtheR"));
        });
    }

    private void testWithNewUsername(String newUserName, Runnable test) {
        String previousUsernameValue = System.getProperty(USER_NAME_SYSTEM_PROPERTY);
        System.setProperty(USER_NAME_SYSTEM_PROPERTY, newUserName);

        test.run();

        System.setProperty(USER_NAME_SYSTEM_PROPERTY, previousUsernameValue);
    }
}
