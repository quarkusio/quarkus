package io.quarkus.kubernetes.client.deployment;

import io.smallrye.common.annotation.Experimental;

@Experimental("This Versions API is experimental")
public class Versions {

  private Versions() {}

  public static final String QUARKUS = "${project.version}";
  public static final String KUBERNETES_CLIENT = "${kubernetes-client.version}";

}
