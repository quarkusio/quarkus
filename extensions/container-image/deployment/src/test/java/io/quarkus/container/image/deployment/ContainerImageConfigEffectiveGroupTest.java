package io.quarkus.container.image.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class ContainerImageConfigEffectiveGroupTest {

    public static final String USER_NAME_SYSTEM_PROPERTY = "user.name";
    private final Optional<String> EMPTY = Optional.empty();

    @Test
    void testSingleSegmentRequestedYieldsEmpty() {
        testWithNewUsername("test", () -> {
            assertEquals(EMPTY,
                    ContainerImageProcessor.getEffectiveGroup(EMPTY, true));
        });
    }

    @Test
    void testExplicitGroupOverridesUsername() {
        testWithNewUsername("test", () -> {
            Optional<String> group = Optional.of("other");
            assertEquals(Optional.of("other"),
                    ContainerImageProcessor.getEffectiveGroup(group, false));
        });
    }

    @Test
    void testExplicitGroupIsLowercasedButNotSanitized() {
        testWithNewUsername("test", () -> {
            Optional<String> group = Optional.of("USER@domain.com");
            assertEquals(Optional.of("user@domain.com"),
                    ContainerImageProcessor.getEffectiveGroup(group, false));
        });
    }

    @Test
    void testFromLowercaseUsername() {
        testWithNewUsername("test", () -> {
            assertEquals(Optional.of("test"),
                    ContainerImageProcessor.getEffectiveGroup(EMPTY, false));
        });
    }

    @Test
    void testFromUppercaseUsername() {
        testWithNewUsername("TEST", () -> {
            assertEquals(Optional.of("test"),
                    ContainerImageProcessor.getEffectiveGroup(EMPTY, false));
        });
    }

    @Test
    void testPreservesDotsUnderscoresAndHyphensInUsername() {
        testWithNewUsername("t.e-s_t", () -> {
            assertEquals(Optional.of("t.e-s_t"),
                    ContainerImageProcessor.getEffectiveGroup(EMPTY, false));
        });
    }

    @Test
    void testPreservesDigitsInUsername() {
        testWithNewUsername("t3st", () -> {
            assertEquals(Optional.of("t3st"),
                    ContainerImageProcessor.getEffectiveGroup(EMPTY, false));
        });
    }

    @Test
    void testFromSpaceInUsername() {
        testWithNewUsername("test user", () -> {
            assertEquals(Optional.of("test-user"),
                    ContainerImageProcessor.getEffectiveGroup(EMPTY, false));
        });
    }

    @Test
    void testFromFullyQualifiedDomainNameInUsername() {
        testWithNewUsername("test@domain.com", () -> {
            assertEquals(Optional.of("test-domain.com"),
                    ContainerImageProcessor.getEffectiveGroup(EMPTY, false));
        });
    }

    @Test
    void testCollapsesRepeatedUnderscores() {
        testWithNewUsername("test__user", () -> {
            assertEquals(Optional.of("test_user"),
                    ContainerImageProcessor.getEffectiveGroup(EMPTY, false));
        });
    }

    @Test
    void testCollapsesRepeatedHyphens() {
        testWithNewUsername("test--user", () -> {
            assertEquals(Optional.of("test-user"),
                    ContainerImageProcessor.getEffectiveGroup(EMPTY, false));
        });
    }

    @Test
    void testCollapsesRepeatedDots() {
        testWithNewUsername("test..user", () -> {
            assertEquals(Optional.of("test.user"),
                    ContainerImageProcessor.getEffectiveGroup(EMPTY, false));
        });
    }

    @Test
    void testReplacesRunOfInvalidCharactersWithSingleDash() {
        testWithNewUsername("test@#*user", () -> {
            assertEquals(Optional.of("test-user"),
                    ContainerImageProcessor.getEffectiveGroup(EMPTY, false));
        });
    }

    @Test
    void testTrimsLeadingSeparators() {
        testWithNewUsername("__test", () -> {
            assertEquals(Optional.of("test"),
                    ContainerImageProcessor.getEffectiveGroup(EMPTY, false));
        });
    }

    @Test
    void testTrimsTrailingSeparators() {
        testWithNewUsername("test__", () -> {
            assertEquals(Optional.of("test"),
                    ContainerImageProcessor.getEffectiveGroup(EMPTY, false));
        });
    }

    @Test
    void testTrimsLeadingAndTrailingMixedSeparators() {
        testWithNewUsername("_-test-_", () -> {
            assertEquals(Optional.of("test"),
                    ContainerImageProcessor.getEffectiveGroup(EMPTY, false));
        });
    }

    @Test
    void testSeparatorOnlyUsernameYieldsEmpty() {
        testWithNewUsername("._-", () -> {
            assertEquals(EMPTY,
                    ContainerImageProcessor.getEffectiveGroup(EMPTY, false));
        });
    }

    private void testWithNewUsername(String newUserName, Runnable test) {
        String previousUsernameValue = System.getProperty(USER_NAME_SYSTEM_PROPERTY);
        System.setProperty(USER_NAME_SYSTEM_PROPERTY, newUserName);

        test.run();

        System.setProperty(USER_NAME_SYSTEM_PROPERTY, previousUsernameValue);
    }
}
