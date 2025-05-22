package io.quarkus.container.image.runtime.dev.ui;

import java.util.Map;

import io.quarkus.dev.console.DevConsoleManager;

public class ContainerBuilderJsonRpcService {

    public String build(String type, String builder) {
        Map<String, String> params = Map.of(
                "quarkus.container-image.builder", builder,
                "quarkus.build.package-type", type);

        return DevConsoleManager.invoke("container-image-build-action", params);
    }

}
