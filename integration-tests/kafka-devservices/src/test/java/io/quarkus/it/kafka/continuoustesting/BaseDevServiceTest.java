package io.quarkus.it.kafka.continuoustesting;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.testcontainers.DockerClientFactory;

import com.github.dockerjava.api.model.Container;

import io.quarkus.devservices.common.Labels;
import io.quarkus.runtime.LaunchMode;

public abstract class BaseDevServiceTest {

    static List<Container> getAllContainers() {
        return DockerClientFactory.lazyClient().listContainersCmd().exec().stream()
                .toList();
    }

    static List<Container> getAllContainers(LaunchMode launchMode) {
        return DockerClientFactory.lazyClient().listContainersCmd().exec().stream()
                .filter(container -> getLaunchMode(container) == launchMode)
                .toList();
    }

    static List<Container> getKafkaContainers() {
        return DockerClientFactory.lazyClient().listContainersCmd().exec().stream()
                .filter(BaseDevServiceTest::isKafkaContainer)
                .toList();
    }

    static List<Container> getKafkaContainers(LaunchMode launchMode) {
        return DockerClientFactory.lazyClient().listContainersCmd().exec().stream()
                .filter(BaseDevServiceTest::isKafkaContainer)
                .filter(container -> getLaunchMode(container) == launchMode)
                .toList();
    }

    static void stopAllContainers() {
        DockerClientFactory.lazyClient().listContainersCmd().exec().stream()
                .filter(BaseDevServiceTest::isKafkaContainer)
                .forEach(c -> DockerClientFactory.lazyClient().stopContainerCmd(c.getId()).exec());
    }

    static List<Container> getKafkaContainersExcludingExisting(Collection<Container> existingContainers) {
        return getKafkaContainers().stream()
                .filter(container -> existingContainers.stream().noneMatch(
                        existing -> existing.getId().equals(container.getId())))
                .toList();
    }

    static List<Container> getKafkaContainersExcludingExisting(LaunchMode launchMode,
            Collection<Container> existingContainers) {
        return getKafkaContainers(launchMode).stream()
                .filter(container -> existingContainers.stream().noneMatch(
                        existing -> existing.getId().equals(container.getId())))
                .toList();
    }

    static List<Container> getAllContainersExcludingExisting(Collection<Container> existingContainers) {
        return getAllContainers().stream().filter(
                container -> existingContainers.stream().noneMatch(
                        existing -> existing.getId().equals(container.getId())))
                .toList();
    }

    static List<Container> getAllContainersExcludingExisting(LaunchMode launchMode, Collection<Container> existingContainers) {
        return getAllContainers(launchMode).stream().filter(
                container -> existingContainers.stream().noneMatch(
                        existing -> existing.getId().equals(container.getId())))
                .toList();
    }

    static LaunchMode getLaunchMode(Container container) {
        String launchMode = container.getLabels().get(Labels.QUARKUS_LAUNCH_MODE);
        return launchMode == null ? null : LaunchMode.valueOf(launchMode.toUpperCase());
    }

    static boolean isKafkaContainer(Container container) {
        // This could be redpanda or kafka-native or other variants
        return container.getImage().contains("kafka") || container.getImage().contains("redpanda");
    }

    static String prettyPrintContainerList(List<Container> newContainers) {
        return newContainers.stream()
                .map(c -> Arrays.toString(c.getPorts()) + " -- " + Arrays.toString(c.getNames()) + " -- " + c.getLabels())
                .collect(Collectors.joining(", \n"));
    }

    static boolean hasPublicPort(Container newContainer, int newPort) {
        return Arrays.stream(newContainer.getPorts()).anyMatch(p -> p.getPublicPort() == newPort);
    }

}
