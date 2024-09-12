package io.quarkus.kubernetes.deployment;

public interface Exposable {
    boolean expose();

    String targetPort();
}
