package io.quarkus.container.image.runtime.dev.ui;

import java.util.Map;

import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.runtime.annotations.JsonRpcDescription;

public class ContainerBuilderJsonRpcService {

    @JsonRpcDescription("This method build a specific container")
    public String build(@JsonRpcDescription("The type of build, valid value are `jar`, `mutable-jar` or `native`") String type,
            @JsonRpcDescription("The builder to use. Valid available builders is in the mcp resource `quarkus-container-image_builderTypes`") String builder) {
        Map<String, String> params = Map.of(
                "quarkus.container-image.builder", builder,
                "quarkus.build.package-type", type);

        return DevConsoleManager.invoke("container-image-build-action", params);
    }

}
