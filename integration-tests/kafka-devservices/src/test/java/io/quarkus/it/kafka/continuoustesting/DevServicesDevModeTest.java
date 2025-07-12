package io.quarkus.it.kafka.continuoustesting;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.DockerClientFactory;

import com.github.dockerjava.api.model.Container;

import io.quarkus.it.kafka.BundledEndpoint;
import io.quarkus.it.kafka.KafkaAdminManager;
import io.quarkus.it.kafka.KafkaAdminTest;
import io.quarkus.it.kafka.KafkaEndpoint;
import io.quarkus.test.QuarkusDevModeTest;

public class DevServicesDevModeTest {

    @RegisterExtension
    public static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .deleteClass(KafkaEndpoint.class)
                    .addClass(BundledEndpoint.class)
                    .addClass(KafkaAdminManager.class)
                    .addAsResource(new StringAsset("quarkus.kafka.devservices.provider=kafka-native\n" +
                            "quarkus.kafka.devservices.topic-partitions.test=2\n"), "application.properties"))
            .setTestArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(KafkaAdminTest.class));

    @Test
    public void testDevModeServiceUpdatesContainersOnConfigChange() {
        // Interacting with the app will force a refresh
        // Note that driving continuous testing concurrently can sometimes cause 500s caused by containers not yet being available on slow machines
        ping();
        List<Container> started = getKafkaContainers();

        assertFalse(started.isEmpty());
        Container container = started.get(0);
        assertTrue(Arrays.stream(container.getPorts()).noneMatch(p -> p.getPublicPort() == 6377),
                "Expected random port, but got: " + Arrays.toString(container.getPorts()));

        int newPort = 6388;
        test.modifyResourceFile("application.properties", s -> s + "quarkus.kafka.devservices.port=" + newPort);

        // Force another refresh
        ping();

        List<Container> newContainers = getKafkaContainersExcludingExisting(started);

        // We expect 1 new containers, since test was not refreshed.
        // On some VMs that's what we get, but on others, a test-mode augmentation happens, and then we get two containers
        assertEquals(1, newContainers.size(),
                "There were " + newContainers.size() + " new containers, and should have been 1 or 2. New containers: "
                        + prettyPrintContainerList(newContainers)
                        + "\n Old containers: " + prettyPrintContainerList(started) + "\n All containers: "
                        + prettyPrintContainerList(getAllContainers())); // this can be wrong
        // We need to inspect the dev-mode container; we don't have a non-brittle way of distinguishing them, so just look in them all
        boolean hasRightPort = newContainers.stream()
                .anyMatch(newContainer -> hasPublicPort(newContainer, newPort));
        assertTrue(hasRightPort,
                "Expected port " + newPort + ", but got: "
                        + newContainers.stream().map(c -> Arrays.toString(c.getPorts())).collect(Collectors.joining(", ")));
    }

    @Test
    public void testDevModeServiceDoesNotRestartContainersOnCodeChange() {
        ping();
        List<Container> started = getKafkaContainers();

        assertFalse(started.isEmpty());
        Container container = started.get(0);
        assertTrue(Arrays.stream(container.getPorts()).noneMatch(p -> p.getPublicPort() == 6377),
                "Expected random port 6377, but got: " + Arrays.toString(container.getPorts()));

        // Make a change that shouldn't affect dev services
        test.modifySourceFile(BundledEndpoint.class, s -> s.replaceAll("topic", "tropic"));

        ping();

        List<Container> newContainers = getKafkaContainersExcludingExisting(started);

        // No new containers should have spawned
        assertEquals(0, newContainers.size(),
                "New containers: " + newContainers + "\n Old containers: " + started + "\n All containers: "
                        + getAllContainers()); // this can be wrong
    }

    @Test
    public void testDevModeKeepsSameInstanceWhenRefreshedOnSecondChange() {
        // Step 1: Ensure we have a dev service running
        System.out.println("Step 1: Ensure we have a dev service running");
        ping();
        List<Container> step1Containers = getKafkaContainers();
        assertFalse(step1Containers.isEmpty());
        Container container = step1Containers.get(0);
        assertFalse(hasPublicPort(container, 6377));

        // Step 2: Make a change that should affect dev services
        System.out.println("Step 2: Make a change that should affect dev services");
        int someFixedPort = 36377;
        // Make a change that SHOULD affect dev services
        test.modifyResourceFile("application.properties",
                s -> s
                        + "quarkus.kafka.devservices.port=" + someFixedPort + "\n");

        ping();

        List<Container> step2Containers = getKafkaContainersExcludingExisting(step1Containers);

        // New containers should have spawned
        assertEquals(1, step2Containers.size(),
                "New containers: " + step2Containers + "\n Old containers: " + step1Containers + "\n All containers: "
                        + getAllContainers());

        assertTrue(hasPublicPort(step2Containers.get(0), someFixedPort));

        // Step 3: Now change back to a random port, which should cause a new container to spawn
        System.out.println("Step 3: Now change back to a random port, which should cause a new container to spawn");
        test.modifyResourceFile("application.properties",
                s -> s.replaceAll("quarkus.kafka.devservices.port=" + someFixedPort, ""));

        ping();

        List<Container> step3Containers = getKafkaContainersExcludingExisting(step2Containers);

        // New containers should have spawned
        assertEquals(1, step3Containers.size(),
                "New containers: " + step3Containers + "\n Old containers: " + step2Containers + "\n All containers: "
                        + getAllContainers());

        // Step 4: Now make a change that should not affect dev services
        System.out.println("Step 4: Now make a change that should not affect dev services");
        test.modifySourceFile(BundledEndpoint.class, s -> s.replaceAll("topic", "tropic"));

        ping();

        List<Container> step4Containers = getKafkaContainersExcludingExisting(step3Containers);

        // No new containers should have spawned
        assertEquals(0, step4Containers.size(),
                "New containers: " + step4Containers + "\n Old containers: " + step3Containers + "\n All containers: "
                        + getAllContainers()); // this can be wrong

        // Step 5: Now make a change that should not affect dev services, but is not the same as the previous change
        System.out.println(
                "Step 5: Now make a change that should not affect dev services, but is not the same as the previous change");
        test.modifySourceFile(BundledEndpoint.class, s -> s.replaceAll("tropic", "topic"));

        ping();

        List<Container> step5Containers = getKafkaContainersExcludingExisting(step3Containers);

        // No new containers should have spawned
        assertEquals(0, step5Containers.size(),
                "New containers: " + step5Containers + "\n Old containers: " + step5Containers + "\n All containers: "
                        + getAllContainers()); // this can be wrong
    }

    private static List<Container> getAllContainers() {
        return DockerClientFactory.lazyClient().listContainersCmd().exec().stream()
                .filter(container -> isKafkaContainer(container)).toList();
    }

    private static List<Container> getKafkaContainers() {
        return getAllContainers();
    }

    private static List<Container> getKafkaContainersExcludingExisting(Collection<Container> existingContainers) {
        return getKafkaContainers().stream().filter(
                container -> existingContainers.stream().noneMatch(existing -> existing.getId().equals(container.getId())))
                .toList();
    }

    private static List<Container> getAllContainersExcludingExisting(Collection<Container> existingContainers) {
        return getAllContainers().stream().filter(
                container -> existingContainers.stream().noneMatch(existing -> existing.getId().equals(container.getId())))
                .toList();
    }

    private static boolean isKafkaContainer(Container container) {
        // This could be redpanda or kafka-native or other variants
        return container.getImage().contains("kafka") || container.getImage().contains("redpanda");
    }

    private static String prettyPrintContainerList(List<Container> newContainers) {
        return newContainers.stream()
                .map(c -> Arrays.toString(c.getPorts()) + " -- " + Arrays.toString(c.getNames()) + " -- " + c.getLabels())
                .collect(Collectors.joining(", \n"));
    }

    private static boolean hasPublicPort(Container newContainer, int newPort) {
        return Arrays.stream(newContainer.getPorts()).anyMatch(p -> p.getPublicPort() == newPort);
    }

    void ping() {
        when().get("/kafka/partitions/test").then()
                .statusCode(200)
                .body(is("2"));
    }

}
