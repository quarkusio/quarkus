package io.quarkus.redis.devservices.continuoustesting.it;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.DockerClientFactory;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;

import io.quarkus.redis.devservices.it.PlainQuarkusTest;
import io.quarkus.test.ContinuousTestingTestUtils;
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.devservices.redis.BundledResource;

/**
 * Note that if this test is specifically selected on the command line with -Dtest=DevServicesRedisContinuousTestingTest, that
 * will override the maven executions and cause it to run twice.
 * That doesn't help debug anything.
 */
public class DevServicesRedisContinuousTestingTest {

    static final String DEVSERVICES_DISABLED_PROPERTIES = ContinuousTestingTestUtils.appProperties(
            "quarkus.devservices.enabled=false");

    static final String FIXED_PORT_PROPERTIES = ContinuousTestingTestUtils.appProperties(
            "quarkus.redis.devservices.port=6377");

    static final String UPDATED_FIXED_PORT_PROPERTIES = ContinuousTestingTestUtils.appProperties(
            "quarkus.redis.devservices.port=6342");

    @RegisterExtension
    public static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(BundledResource.class)
                    .addAsResource(new StringAsset(ContinuousTestingTestUtils.appProperties("")),
                            "application.properties"))
            .setTestArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(PlainQuarkusTest.class));

    @Disabled("Not currently working")
    @Test
    public void testContinuousTestingDisablesDevServicesWhenPropertiesChange() {
        ContinuousTestingTestUtils utils = new ContinuousTestingTestUtils();
        var result = utils.waitForNextCompletion();
        assertEquals(1, result.getTotalTestsPassed());
        assertEquals(0, result.getTotalTestsFailed());

        // Now let's disable dev services globally ... BOOOOOM! Splat!
        test.modifyResourceFile("application.properties", s -> DEVSERVICES_DISABLED_PROPERTIES);
        result = utils.waitForNextCompletion();
        assertEquals(0, result.getTotalTestsPassed());
        assertEquals(1, result.getTotalTestsFailed());

        // We could check the container goes away, but we'd have to check slowly, because ryuk can be slow
    }

    // This tests behaviour in dev mode proper (rather than continuous testing)
    @Test
    public void testDevModeServiceUpdatesContainersOnConfigChange() {
        List<Container> started = getRedisContainers();
        // Interacting with the app will force a refresh
        ping();

        assertFalse(started.isEmpty());
        Container container = started.get(0);
        assertTrue(Arrays.stream(container.getPorts()).noneMatch(p -> p.getPublicPort() == 6377),
                "Expected random port, but got: " + Arrays.toString(container.getPorts()));

        int newPort = 6388;
        // Continuous tests and dev mode should *not* share containers, even if the port is fixed
        // Specify that the fixed port is for dev mode
        test.modifyResourceFile("application.properties",
                s -> ContinuousTestingTestUtils.appProperties("quarkus.redis.devservices.port=" + newPort));

        // Force another refresh
        ping();
        List<Container> newContainers = getRedisContainersExcludingExisting(started);

        assertEquals(1, newContainers.size(),
                "New containers: "
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
        List<Container> started = getRedisContainers();
        ping();

        assertFalse(started.isEmpty());
        Container container = started.get(0);
        assertTrue(Arrays.stream(container.getPorts()).noneMatch(p -> p.getPublicPort() == 6377),
                "Expected random port 6377, but got: " + Arrays.toString(container.getPorts()));

        // Make a change that shouldn't affect dev services
        test.modifySourceFile(BundledResource.class, s -> s.replaceAll("OK", "poink"));

        ping();

        List<Container> newContainers = getRedisContainersExcludingExisting(started);

        // No new containers should have spawned
        assertEquals(0, newContainers.size(),
                "New containers: " + newContainers + "\n Old containers: " + started + "\n All containers: "
                        + getAllContainers()); // this can be wrong
    }

    private static String prettyPrintContainerList(List<Container> newContainers) {
        return newContainers.stream()
                .map(c -> Arrays.toString(c.getPorts()) + "--" + Arrays.toString(c.getNames()))
                .collect(Collectors.joining(", "));
    }

    private static boolean hasPublicPort(Container newContainer, int newPort) {
        return Arrays.stream(newContainer.getPorts()).anyMatch(p -> p.getPublicPort() == newPort);
    }

    void ping() {
        when().get("/bundled/ping").then()
                .statusCode(200)
                .body(is("PONG"));
    }

    @Test
    public void testContinuousTestingReusesInstanceWhenPropertiesAreNotChanged() {

        ContinuousTestingTestUtils utils = new ContinuousTestingTestUtils();
        var result = utils.waitForNextCompletion();
        assertEquals(1, result.getTotalTestsPassed());
        assertEquals(0, result.getTotalTestsFailed());
        List<Container> redisContainers = getRedisContainers();

        // Make a change that shouldn't affect dev services
        test.modifyTestSourceFile(PlainQuarkusTest.class, s -> s.replaceAll("redisClient", "updatedRedisClient"));

        result = utils.waitForNextCompletion();
        assertEquals(1, result.getTestsPassed());
        assertEquals(0, result.getTestsFailed());

        // Some containers could have disappeared, because ryuk cleaned them up, but no new containers should have appeared
        List<Container> newContainers = getRedisContainersExcludingExisting(redisContainers);
        assertEquals(0, newContainers.size(),
                "New containers: " + newContainers + "\n Old containers: " + redisContainers + "\n All containers: "
                        + getAllContainers());
    }

    @Test
    public void testContinuousTestingCreatesANewInstanceWhenPropertiesAreChanged() {

        ContinuousTestingTestUtils utils = new ContinuousTestingTestUtils();
        var result = utils.waitForNextCompletion();
        assertEquals(1, result.getTotalTestsPassed());
        assertEquals(0, result.getTotalTestsFailed());
        List<Container> existingContainers = new ArrayList<>();
        existingContainers.addAll(getRedisContainers());

        test.modifyResourceFile("application.properties", s -> FIXED_PORT_PROPERTIES);

        result = utils.waitForNextCompletion();
        assertEquals(1, result.getTestsPassed());
        assertEquals(0, result.getTestsFailed());

        // A new container should have appeared
        {
            List<Container> newContainers = getRedisContainersExcludingExisting(existingContainers);
            existingContainers.addAll(newContainers);
            assertEquals(1, newContainers.size(),
                    "New containers: " + newContainers + "\n Old containers: " + existingContainers + "\n All containers: "
                            + getAllContainers());

            // The new container should be on the new port
            List<Integer> ports = Arrays.stream(newContainers.get(0).getPorts())
                    .map(ContainerPort::getPublicPort)
                    .toList();

            // Oh good, it's one port, so it should be the expected one
            assertTrue(ports.contains(6377), "Container ports: " + ports);
        }
        test.modifyResourceFile("application.properties", s -> UPDATED_FIXED_PORT_PROPERTIES);

        result = utils.waitForNextCompletion();
        assertEquals(1, result.getTestsPassed());
        assertEquals(0, result.getTestsFailed());

        // Another new container should have appeared

        {
            List<Container> newContainers = getRedisContainersExcludingExisting(existingContainers);
            assertEquals(1, newContainers.size(),
                    "New containers: " + newContainers + "\n Old containers: " + existingContainers + "\n All containers: "
                            + getAllContainers());

            // The new container should be on the new port
            List<Integer> ports = Arrays.stream(newContainers.get(0).getPorts())
                    .map(ContainerPort::getPublicPort)
                    .toList();
            assertTrue(ports.contains(6342), "Container ports: " + ports);

        }
    }

    private static List<Container> getAllContainers() {
        return DockerClientFactory.lazyClient().listContainersCmd().exec().stream()
                .filter(container -> isRedisContainer(container)).toList();
    }

    private static List<Container> getRedisContainers() {
        return getAllContainers();
    }

    private static List<Container> getRedisContainersExcludingExisting(Collection<Container> existingContainers) {
        return getRedisContainers().stream().filter(
                container -> existingContainers.stream().noneMatch(existing -> existing.getId().equals(container.getId())))
                .toList();
    }

    private static boolean isRedisContainer(Container container) {
        // The output of getCommand() seems to vary by host OS (it's different on CI and mac), but the image name should be reliable
        return container.getImage().contains("redis");
    }
}
