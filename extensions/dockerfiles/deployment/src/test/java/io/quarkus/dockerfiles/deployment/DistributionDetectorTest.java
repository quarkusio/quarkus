package io.quarkus.dockerfiles.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.dockerfiles.spi.Distribution;

public class DistributionDetectorTest {

    @Test
    public void shouldDetectUBI() {
        assertEquals(Distribution.UBI,
                DistributionDetector.detectDistribution("registry.access.redhat.com/ubi8/openjdk-21:1.19"));
        assertEquals(Distribution.UBI, DistributionDetector.detectDistribution("ubi8/openjdk-17"));
        assertEquals(Distribution.UBI, DistributionDetector.detectDistribution("registry.redhat.io/ubi9/ubi"));
    }

    @Test
    public void shouldDetectUbuntu() {
        assertEquals(Distribution.UBUNTU, DistributionDetector.detectDistribution("ubuntu:22.04"));
        assertEquals(Distribution.UBUNTU, DistributionDetector.detectDistribution("ubuntu:latest"));
    }

    @Test
    public void shouldDetectDebian() {
        assertEquals(Distribution.DEBIAN, DistributionDetector.detectDistribution("debian:bullseye"));
        assertEquals(Distribution.DEBIAN, DistributionDetector.detectDistribution("debian:latest"));
    }

    @Test
    public void shouldDetectAlpine() {
        assertEquals(Distribution.ALPINE, DistributionDetector.detectDistribution("alpine:3.17"));
        assertEquals(Distribution.ALPINE, DistributionDetector.detectDistribution("alpine:latest"));
    }

    @Test
    public void shouldDetectFedora() {
        assertEquals(Distribution.FEDORA, DistributionDetector.detectDistribution("fedora:37"));
        assertEquals(Distribution.FEDORA, DistributionDetector.detectDistribution("fedora:latest"));
    }

    @Test
    public void shouldDetectRHEL() {
        assertEquals(Distribution.RHEL, DistributionDetector.detectDistribution("rhel8/nodejs-16"));
        assertEquals(Distribution.RHEL, DistributionDetector.detectDistribution("centos:7"));
        assertEquals(Distribution.RHEL, DistributionDetector.detectDistribution("rockylinux:9"));
        assertEquals(Distribution.RHEL, DistributionDetector.detectDistribution("almalinux:9"));
    }

    @Test
    public void shouldDetectUnknown() {
        assertEquals(Distribution.UNKNOWN, DistributionDetector.detectDistribution("scratch"));
        assertEquals(Distribution.UNKNOWN, DistributionDetector.detectDistribution("custom/myimage:latest"));
        assertEquals(Distribution.UNKNOWN, DistributionDetector.detectDistribution(""));
        assertEquals(Distribution.UNKNOWN, DistributionDetector.detectDistribution(null));
    }

    @Test
    public void shouldHandleRegistryPrefixes() {
        assertEquals(Distribution.UBUNTU, DistributionDetector.detectDistribution("docker.io/ubuntu:22.04"));
        assertEquals(Distribution.ALPINE, DistributionDetector.detectDistribution("quay.io/alpine:latest"));
        assertEquals(Distribution.UBI, DistributionDetector.detectDistribution("registry.access.redhat.com/ubi8/nodejs-16"));
    }
}
