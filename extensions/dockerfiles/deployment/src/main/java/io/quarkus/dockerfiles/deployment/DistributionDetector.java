package io.quarkus.dockerfiles.deployment;

import java.util.Locale;

import io.quarkus.dockerfiles.spi.Distribution;

/**
 * Utility class to detect the Linux distribution from Docker base image names.
 * Uses common patterns in image names to determine the appropriate package manager.
 */
public class DistributionDetector {

    /**
     * Detect the distribution from a Docker FROM image string.
     *
     * @param fromImage the FROM image (e.g., "registry.access.redhat.com/ubi8/openjdk-17:1.18")
     * @return the detected Distribution, or Distribution.UNKNOWN if not recognized
     */
    public static Distribution detectDistribution(String fromImage) {
        if (fromImage == null || fromImage.trim().isEmpty()) {
            return Distribution.UNKNOWN;
        }

        // Convert to lowercase for case-insensitive matching
        String image = fromImage.toLowerCase(Locale.ROOT);

        // Remove registry prefixes to focus on the image name
        String imageName = extractImageName(image);

        // UBI (Red Hat Universal Base Image) patterns
        if (imageName.contains("ubi") ||
                imageName.contains("registry.access.redhat.com") ||
                imageName.contains("registry.redhat.io")) {
            return Distribution.UBI;
        }

        // Fedora patterns
        if (imageName.contains("fedora")) {
            return Distribution.FEDORA;
        }

        // RHEL/CentOS patterns
        if (imageName.contains("rhel") ||
                imageName.contains("centos") ||
                imageName.contains("rockylinux") ||
                imageName.contains("almalinux")) {
            return Distribution.RHEL;
        }

        // Ubuntu patterns
        if (imageName.contains("ubuntu")) {
            return Distribution.UBUNTU;
        }

        // Debian patterns
        if (imageName.contains("debian")) {
            return Distribution.DEBIAN;
        }

        // Alpine patterns
        if (imageName.contains("alpine")) {
            return Distribution.ALPINE;
        }

        return Distribution.UNKNOWN;
    }

    /**
     * Extract the image name from a full Docker image reference.
     * Removes registry hostname and focuses on the image name portion.
     *
     * @param fullImage the full image reference
     * @return the image name portion
     */
    private static String extractImageName(String fullImage) {
        // Handle registry.com/namespace/image:tag format
        String[] parts = fullImage.split("/");
        if (parts.length > 1) {
            // If it contains a registry (has dots or localhost), skip the first part
            if (parts[0].contains(".") || parts[0].contains("localhost")) {
                // Join the remaining parts
                return String.join("/", java.util.Arrays.copyOfRange(parts, 1, parts.length));
            }
        }
        return fullImage;
    }
}