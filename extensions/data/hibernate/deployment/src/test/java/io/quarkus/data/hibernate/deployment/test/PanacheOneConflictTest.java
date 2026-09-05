package io.quarkus.data.hibernate.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Quarkus Data (Panache Next) and classic Panache (Panache 1) cannot be used together. When both are on the classpath
 * the build must fail early with a clear message rather than with a cryptic error caused by their overlapping setup.
 */
public class PanacheOneConflictTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            // Force the classic Panache (Panache 1) extension onto this single test's classpath, next to Quarkus Data.
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-hibernate-orm-panache-deployment", Version.getVersion())))
            .assertException(throwable -> {
                String messages = collectMessages(throwable);
                assertThat(messages)
                        .contains("Quarkus Data")
                        .contains("classic Panache")
                        .contains("cannot be used together")
                        .contains("quarkus-data-hibernate")
                        .contains("hibernate-orm-panache");
            })
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-test.properties", "application.properties")
                    .addClasses(MyEntity.class));

    @Test
    void conflictShouldFailTheBuild() {
        Assertions.fail("The build should have failed because classic Panache and Quarkus Data are both present");
    }

    private static String collectMessages(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        List<Throwable> seen = new ArrayList<>();
        Throwable current = throwable;
        while (current != null && !seen.contains(current)) {
            seen.add(current);
            if (current.getMessage() != null) {
                builder.append(current.getMessage()).append('\n');
            }
            for (Throwable suppressed : current.getSuppressed()) {
                if (suppressed.getMessage() != null) {
                    builder.append(suppressed.getMessage()).append('\n');
                }
            }
            current = current.getCause();
        }
        return builder.toString();
    }
}
