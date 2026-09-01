package io.quarkus.it.aesh;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.aesh.AeshLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

/**
 * Tests REPL mode with Hibernate ORM and H2 on the classpath.
 * <p>
 * This is a "realistic" test that verifies AeshLauncher works when
 * common extensions (JTA, Hibernate, JDBC) are present -- catching
 * classloader and augmentation issues that don't surface with
 * minimal aesh-only tests.
 */
@QuarkusMainTest
public class AeshReplWithHibernateTest {

    @Test
    void testAddAndListItems(AeshLauncher launcher) {
        // Database starts empty
        launcher.execute("list-items");
        assertThat(launcher.getCommandOutput()).contains("No items found");

        // Add items
        launcher.execute("add-item apple");
        assertThat(launcher.getCommandOutput()).isEqualTo("Added: apple\n");

        launcher.execute("add-item banana");
        assertThat(launcher.getCommandOutput()).isEqualTo("Added: banana\n");

        // Verify persistence — multi-line output, use contains for individual lines
        launcher.execute("list-items");
        assertThat(launcher.getCommandOutput())
                .contains("Items (2):")
                .contains("- apple")
                .contains("- banana");
    }

    @Test
    void testDataIsolationBetweenTests(AeshLauncher launcher) {
        // Each test gets a fresh database (drop-and-create)
        launcher.execute("list-items");
        assertThat(launcher.getCommandOutput()).contains("No items found");

        launcher.execute("add-item cherry");
        assertThat(launcher.getCommandOutput()).isEqualTo("Added: cherry\n");

        launcher.execute("list-items");
        assertThat(launcher.getCommandOutput())
                .contains("Items (1):")
                .contains("- cherry");
    }
}
