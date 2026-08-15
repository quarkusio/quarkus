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
        launcher.launch();

        // Database starts empty
        String output = launcher.executeCommand("list-items");
        assertThat(output).contains("No items found");

        // Add items
        output = launcher.executeCommand("add-item apple");
        assertThat(output).contains("Added: apple");

        output = launcher.executeCommand("add-item banana");
        assertThat(output).contains("Added: banana");

        // Verify persistence
        output = launcher.executeCommand("list-items");
        assertThat(output).contains("Items (2):");
        assertThat(output).contains("- apple");
        assertThat(output).contains("- banana");

        launcher.exit();
    }

    @Test
    void testDataIsolationBetweenTests(AeshLauncher launcher) {
        launcher.launch();

        // Each test gets a fresh database (drop-and-create)
        String output = launcher.executeCommand("list-items");
        assertThat(output).contains("No items found");

        output = launcher.executeCommand("add-item cherry");
        assertThat(output).contains("Added: cherry");

        output = launcher.executeCommand("list-items");
        assertThat(output).contains("Items (1):");
        assertThat(output).contains("- cherry");

        launcher.exit();
    }
}
