package io.quarkus.jfr.runtime.internal.runtime;

import java.util.List;

public interface QuarkusRuntimeInfo {

    String imageMode();

    String profiles();

    String version();

    List<String> features();
}
